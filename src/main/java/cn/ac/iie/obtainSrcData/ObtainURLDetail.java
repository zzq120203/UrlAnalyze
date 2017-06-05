package cn.ac.iie.obtainSrcData;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.ac.iie.Entity.GlobalConfig;
import cn.ac.iie.Entity.URLDetailEntity;

public class ObtainURLDetail implements Runnable {

	private static Logger logger;
	private GlobalConfig gConfig;
	private static ObjectMapper mapper = new ObjectMapper();
	private ArrayList<LinkedBlockingQueue<URLDetailEntity>> outputURLDBlockingQList;
	private AtomicLong obtain_nr = new AtomicLong(0);
	private AtomicLong webfilter_nr = new AtomicLong(0);

	public ObtainURLDetail(GlobalConfig globalConfig) {
		gConfig = globalConfig;
		logger = LoggerFactory.getLogger(ObtainURLDetail.class);
		outputURLDBlockingQList = new ArrayList<LinkedBlockingQueue<URLDetailEntity>>();
		for (int i = 0; i < gConfig.maxBlockQueue; i++) {
			LinkedBlockingQueue<URLDetailEntity> dxoBQueue = new LinkedBlockingQueue<URLDetailEntity>(
					gConfig.maxBlockingQueueCapacity);
			outputURLDBlockingQList.add(dxoBQueue);
		}
	}

	public ArrayList<LinkedBlockingQueue<URLDetailEntity>> getOutputURLDBlockingQList() {
		return outputURLDBlockingQList;
	}

	public long getObtainNr() {
		return obtain_nr.get();
	}

	public long getFilter_Nr() {
		return webfilter_nr.get();
	}

	public String generate_dn(String url) {
		String dnString = null;
		try {
			url = url.replaceAll("\\^", "");
			URI u = new URI(url);
			dnString = u.getHost();
		} catch (URISyntaxException e1) {
			if (dnString == null) {
				int index = url.indexOf("://");
				if (index != -1)
					dnString = url.substring(index + 3);
				index = dnString.indexOf("/");
				if (index != -1)
					dnString = dnString.substring(0, index);
			}
		}
		return dnString;
	}

	@Override
	public void run() {
		String detailFilePath = gConfig.DetailFilePath;
		int count = -1;
		while (!gConfig.stopDetailObtainThread) {
			File dir = new File(detailFilePath);
			try {
				if (!dir.isDirectory()) {
					logger.error(dir.getPath() + "is not exist");
//					System.out.println("path=" + dir.getPath());
//					System.out.println("absolutepath=" + dir.getAbsolutePath());
//					System.out.println("name=" + dir.getName());
					
				} else if (dir.isDirectory()) {
					String[] filelist = dir.list();
					for (int i = 0; i < filelist.length; i++) {
						File readfile = new File(detailFilePath + "/" + filelist[i]);
						try {
							if (readfile.length() == 0)
								continue;
							URLDetailEntity urlde = mapper.readValue(readfile, URLDetailEntity.class);
							count++;
							while (outputURLDBlockingQList.get(count % gConfig.maxBlockQueue).remainingCapacity() <= 0) {
								Thread.sleep(10);
							}
							outputURLDBlockingQList.get(count % gConfig.maxBlockQueue).put(urlde);
							obtain_nr.incrementAndGet();
						} catch (JsonParseException e) {
							moveFile(readfile);
							logger.error(e.getMessage(), e);
						} catch (JsonMappingException e) {
							moveFile(readfile);
							logger.error(e.getMessage(), e);
						} catch (IOException e) {
							moveFile(readfile);
							logger.error(e.getMessage(), e);
						} catch (InterruptedException e) {
							moveFile(readfile);
							logger.error(e.getMessage(), e);
						} finally {
							readfile.delete();
							//System.out.println("我是个空的finally");
						}
					}
				}
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.info("Obtain Thread exitting...");
	}
	
	private boolean moveFile(File afile) {
        File nfile = new File("./ERROR");
        boolean success = afile.renameTo(new File(nfile, afile.getName()));
        return success;

	}
}
