package cn.ac.iie.util;

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.ConnectionException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.OperationTimedOutException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

/**
 * Created by jinsheng on 16/9/18.
 *
 */
public class CassandraConnector {

    protected String tableName;
    private String timestampKey;
    private List<String> updatePrimaryKey;

    private List<ColumnMetadata> columns;
    private Map<String, Type> columnMap = new HashMap<>();

    protected Cluster cluster;
    protected Session session;

    protected PreparedStatement insert_statement = null;
    protected String exists = "";

    private static Logger logger = LoggerFactory.getLogger(CassandraConnector.class);
    private ObjectMapper objectMapper;
    
    public CassandraConnector(String keyspace,
                              String dataCenter,
                              String coordinatorAddress,
                              String userName,
                              String password,
                              String tableName,
                              String timestampKey,
                              List<String> updatePrimaryKey,
                              Integer readTimeOut,
                              Boolean retry) {
        this.tableName = tableName;
        this.timestampKey = timestampKey;
        this.updatePrimaryKey = updatePrimaryKey;
        if(readTimeOut == null) {
            readTimeOut = 12000;
        }

        //创建连接
        QueryOptions options = new QueryOptions();
        options.setConsistencyLevel(ConsistencyLevel.LOCAL_ONE);
        Cluster.Builder builder = Cluster.builder();
        if (userName.length() > 0) //需要用户认证
            builder
                    .withSocketOptions(new SocketOptions().setConnectTimeoutMillis(120000).setReadTimeoutMillis(readTimeOut))
                    .withLoadBalancingPolicy(new TokenAwarePolicy(DCAwareRoundRobinPolicy.builder().withLocalDc(dataCenter).build()))
                    //.withRetryPolicy(DefaultRetryPolicy.INSTANCE)
                    .withCredentials(userName, password).addContactPoint(coordinatorAddress).withQueryOptions(options);
        else // 不需要用户认证
            builder
                    .withSocketOptions(new SocketOptions().setConnectTimeoutMillis(120000).setReadTimeoutMillis(readTimeOut))
                    .withLoadBalancingPolicy(new TokenAwarePolicy(DCAwareRoundRobinPolicy.builder().withLocalDc(dataCenter).build()))
                    //.withRetryPolicy(DefaultRetryPolicy.INSTANCE)
                    .addContactPoint(coordinatorAddress).withQueryOptions(options);
        //设置重试策略
        if(retry == null || retry)
            this.cluster = builder.withRetryPolicy(DefaultRetryPolicy.INSTANCE).build();
        else
            this.cluster = builder.build();
        //绑定keyspace
        this.session = this.cluster.connect(keyspace);
        //获取该表的元数据
        this.columns = this.cluster.getMetadata().getKeyspace(keyspace).getTable(this.tableName).getColumns();

        //处理每一列的类型并保存，用于更新时确定调用哪个set方法
        columnMap = columns.parallelStream()
                .collect(Collectors.toMap(ColumnMetadata::getName, col-> resolveType(col.getType())));
        logger.info("get column type: {}", columnMap);
        objectMapper = new ObjectMapper();
    }
    
    /**
     * 映射Cassandra类型到{@linkplain Type}枚举
     * @param rawType Cassandra类型对象
     * @return 返回Cassandra类型对应的Type枚举
     */
    private Type resolveType(DataType rawType) {
        switch (rawType.getName()) {
            case ASCII:
            case VARCHAR:
            case TEXT:
                return Type.STRING;
            case TINYINT:
                return Type.TINYINT;
            case SMALLINT:
                return Type.SMALLINT;
            case INT:
                return Type.INT;
            case BIGINT:
            case VARINT:
                return Type.BIGINT;
            case FLOAT:
                return Type.FLOAT;
            case DOUBLE:
                return Type.DOUBLE;
            case BLOB:
                return Type.BLOB;
            case BOOLEAN:
                return Type.BOOLEAN;
            case LIST:
                return Type.LIST;
            case MAP:
                return Type.MAP;
            case SET:
                return Type.SET;
            default:
                throw new IllegalArgumentException("Unsupported type: "+rawType);
        }
    }


    public Session getSession() {
        return this.session;
    }

    /**
     * 设置更新条件
     * @return return update cql string
     */
    private String getUpdateStr(MPPConf mc) {
        StringBuilder sb = new StringBuilder();
        // 遍历需要更新的各个字段并写入set子句中
        for (String updateField : mc.getUpdateFields()) {
            sb.append(updateField)
                    .append("=:")
                    .append(updateField)
                    .append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private String getKeyConjunctsStr() {

        StringBuilder sb = new StringBuilder();
        // 遍历需要更新的各个字段并写入where条件中
        for (String key : updatePrimaryKey) {
            sb.append(key)
                    .append("=:")
                    .append(key)
                    .append(" and ");
        }
        return sb.substring(0,sb.length()-5);
    }

    /**
     * 设置更新列的值
     * @param map 从消息队列中读取的数据
     * @param bs bound statement
     */
    private void setUpdateValue(Map map, BoundStatement bs, MPPConf mc) {
        // 遍历需要更新的各个列并分别设置更新值
        for (String updateField : mc.getUpdateFields()) {
            Type type = columnMap.get(updateField);
            Object val = map.get(updateField);
            type.bind(updateField, val, bs);
        }
    }
    /**
     * 设置更新列的值
     * @param map 从消息队列中读取的数据
     * @param bs bound statement
     */
    private void setKeyValue(Map map, BoundStatement bs) {
        // 遍历需要各个主键并分别设置条件值
        for (String updateField : updatePrimaryKey) {
            Type type = columnMap.get(updateField);
            Object val = map.get(updateField);
            type.bind(updateField, val, bs);
        }
    }


    /**
     * 发送数据，传入参数是 List&lt;Map&gt; 类型的
     * @param dataLists data list
     */
    public int pushData(List<Map<String, Object>> dataLists, MPPConf mc) throws IOException {
        return push_multi_json_data(dataLists, mc);
    }

    protected void pushData(Map<String, Object> data, MPPConf mc) throws IOException {

        while(pushSingleData(data, mc)) {
            LockSupport.parkNanos(1000000000);
        }
    }

    private boolean pushSingleData(Map<String, Object> data, MPPConf mc) throws IOException {
        if(mc.isUpdate()) {
            return updateSingleData(data, mc);
        } else {
            return insertSingleData(data, mc);
        }
    }

    /**
     * 提交数据到Cassandra
     *
     * @param data 数据列表
     * @return 返回是否提交成功
     * @throws IOException 抛出异常
     */
    private boolean insertSingleData(Map<String, Object> data, MPPConf mc) throws IOException {
        if(insert_statement == null) {
            // 预编译insert语句
            prepare("INSERT INTO " + this.tableName + " JSON :json " + exists);
        }

        BoundStatement bs;

        // 绑定数据并创建BoundStatement对象
        String json = convertJson(data);
        bs = insert_statement.bind(json);
        //logger.debug(json);
        return executeQuery(data.get(updatePrimaryKey.get(0)), bs);
    }

    protected String convertJson(Map<String, Object> data) throws IOException {
        return objectMapper.writeValueAsString(data);
    }

    private synchronized void prepare(String prepareCql) {
        if(insert_statement == null) {
            insert_statement = session.prepare(prepareCql);
        }
    }

    /**
     * 提交数据到Cassandra
     * @param jsonList 数据列表
     * @return 返回是否提交成功
     * @throws IOException 抛出异常
     */
    private int push_multi_json_data(List<Map<String, Object>> jsonList, MPPConf mc) throws IOException {
        int wrote = 0;
        for (Map<String, Object> data : jsonList) {
            try {
                pushData(data, mc);
                wrote++;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return wrote;
    }

    private boolean updateSingleData(Map<String, Object> data, MPPConf mc) {
        if(insert_statement == null) {
            // 预编译update语句
            String cql = "update " + this.tableName + " set "
                    + getUpdateStr(mc) + " where " + getKeyConjunctsStr()
                    + " IF EXISTS" + exists;// IF EXISTS";
            logger.info("generate prepared sql: {}", cql);
            prepare(cql);
        }


        logger.debug("update data: {}", data);
        //绑定更新数据，并创建BoundStatement对象
        BoundStatement bs = insert_statement.bind();
        setUpdateValue(data, bs, mc);
        // 绑定更新记录的主键
        setKeyValue(data, bs);
        // 设置记录时间戳
        if (this.timestampKey.length() > 0 && data.containsKey(this.timestampKey))
            bs.setDefaultTimestamp((Long) data.get(this.timestampKey) * 1000000); // second to microsecond

        return executeQuery(data.get(updatePrimaryKey.get(0)), bs);
    }

    private boolean executeQuery(Object key, Statement st) {

        try {
            ResultSet rs = session.execute(st);
            String result_string = rs.toString();
            if (result_string.contains("ERROR")) {
                logger.info(result_string);
                //logger.info("records not inserted or indexed : {}", jsonList.get(i));
            }
            logger.debug("executed query for data: {}", key);
        } catch (OperationTimedOutException | QueryExecutionException | NoHostAvailableException e) {
            logger.warn("execute failed ,key: {}, exp: {}", key, e.getMessage());
            return true;
        } catch (ConnectionException e) {
            if(e.getMessage().contains("Connection has been closed")) {
                logger.warn("connection has been closed, waiting retry, key: {}, address: {}",
                        key, e.getHost().getHostAddress());
            }
            return false;
        }

        return false;
    }

    public void internalClose() {
        this.session.close();
        this.cluster.close();
    }

    private enum Type {
        STRING {
            @Override
            public void bind(String col, Object val, BoundStatement bs) {
                if(val == null)
                    bs.setToNull(col);
                else
                    bs.setString(col, val.toString());
            }
        }, TINYINT {
            @Override
            public void bind(String col, Object val, BoundStatement bs) {
                if(val == null)
                    bs.setToNull(col);
                else if(val instanceof String) {
                    bs.setByte(col, Long.valueOf(((String) val).toLowerCase()).byteValue());
                } else if(val instanceof Number) {
                    bs.setByte(col, ((Number)val).byteValue());
                } else {
                    throw new IllegalArgumentException("wrong type of "+ col);
                }
            }
        }, SMALLINT {
            @Override
            public void bind(String col, Object val, BoundStatement bs) {
                if(val == null)
                    bs.setToNull(col);
                else if(val instanceof String) {
                    bs.setShort(col, Long.valueOf(((String) val).toLowerCase()).shortValue());
                } else if(val instanceof Number) {
                    bs.setShort(col, ((Number)val).shortValue());
                } else {
                    throw new IllegalArgumentException("wrong type of "+ col);
                }
            }
        }, INT {
            @Override
            public void bind(String col, Object val, BoundStatement bs) {
                if(val == null)
                    bs.setToNull(col);
                else if(val instanceof String) {
                    bs.setInt(col, Long.valueOf(((String) val).toLowerCase()).intValue());
                } else if(val instanceof Number) {
                    bs.setInt(col, ((Number)val).intValue());
                } else {
                    throw new IllegalArgumentException("wrong type of "+ col);
                }
            }
        }, BIGINT {
            @Override
            public void bind(String col, Object val, BoundStatement bs) {
                if(val == null)
                    bs.setToNull(col);
                else if(val instanceof String) {
                    bs.setLong(col, Long.valueOf(((String) val).toLowerCase()));
                } else if(val instanceof Number) {
                    bs.setLong(col, ((Number)val).longValue());
                } else {
                    throw new IllegalArgumentException("wrong type of "+ col);
                }
            }
        }, FLOAT {
            @Override
            public void bind(String col, Object val, BoundStatement bs) {
                if(val == null)
                    bs.setToNull(col);
                else if(val instanceof String) {
                    bs.setFloat(col, Long.valueOf(((String) val).toLowerCase()).floatValue());
                } else if(val instanceof Number) {
                    bs.setFloat(col, ((Number)val).floatValue());
                } else {
                    throw new IllegalArgumentException("wrong type of "+ col);
                }
            }
        }, DOUBLE {
            @Override
            public void bind(String col, Object val, BoundStatement bs) {
                if(val == null)
                    bs.setToNull(col);
                else if(val instanceof String) {
                    bs.setDouble(col, Long.valueOf(((String) val).toLowerCase()).doubleValue());
                } else if(val instanceof Number) {
                    bs.setDouble(col, ((Number)val).doubleValue());
                } else {
                    throw new IllegalArgumentException("wrong type of "+ col);
                }
            }
        }, BLOB {
            @Override
            public void bind(String col, Object val, BoundStatement bs) {
                if(val == null)
                    bs.setToNull(col);
                else if(val instanceof String) {
                    try {
                        byte[] bytes = Hex.decodeHex(((String)val).toCharArray());
                        bs.setBytes(col, ByteBuffer.wrap(bytes));
                    } catch (DecoderException e) {
                        throw new IllegalArgumentException("wrong value of hex string, column: "+ col);
                    }
                } else if(val instanceof byte[]) {
                    bs.setBytes(col, ByteBuffer.wrap((byte[]) val));
                } else if(val instanceof ByteBuffer) {
                    bs.setBytes(col, (ByteBuffer) val);
                } else {
                    throw new IllegalArgumentException("wrong type of "+ col);
                }
            }
        }, BOOLEAN {
            @Override
            public void bind(String col, Object val, BoundStatement bs) {
                if(val == null)
                    bs.setToNull(col);
                else if(val instanceof String) {
                    bs.setBool(col, Boolean.valueOf((String) val));
                } else if(val instanceof Boolean) {
                    bs.setBool(col, (Boolean) val);
                } else {
                    throw new IllegalArgumentException("wrong type of "+ col);
                }
            }
        }, LIST {
            @Override
            public void bind(String col, Object val, BoundStatement bs) {
                if(val == null)
                    bs.setToNull(col);
                else if(val instanceof List) {
                    bs.setList(col, (List<?>) val);
                } else if(val instanceof Collection) {
                    List<?> l = new ArrayList<>((Collection<?>)val);
                    bs.setList(col, l);
                } else {
                    List<Object> l = new ArrayList<>();
                    l.add(val);
                    bs.setList(col, l);
                }
            }
        }, MAP {
            @Override
            public void bind(String col, Object val, BoundStatement bs) {
                if(val == null) {
                    bs.setToNull(col);
                } else if(val instanceof Map) {
                    bs.setMap(col, (Map<?, ?>) val);
                } else {
                    throw new IllegalArgumentException("wrong type of "+ col);
                }
            }
        }, SET {
            @Override
            public void bind(String col, Object val, BoundStatement bs) {
                if(val == null) {
                    bs.setToNull(col);
                } else if(val instanceof Set){
                    bs.setSet(col, (Set<?>) val);
                } else if( val instanceof Collection) {
                    Set<?> s = new HashSet<>((Collection<?>) val);
                    bs.setSet(col, s);
                } else {
                    Set<Object> s = new HashSet<>();
                    s.add(val);
                    bs.setSet(col, s);
                }
            }
        };

        public void bind(String col, Object val, BoundStatement bs) {
            throw new IllegalArgumentException("wrong type of "+ col);
        }
    }

    @JsonProperty("exists")
    public String getExists() {
        return exists;
    }

    public void setExists(String exists) {
        this.exists = exists;
    }
}
