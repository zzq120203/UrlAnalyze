package cn.ac.iie.Process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class LogicSyntaxTree {

	private static final Map<String, Integer> operatorPriority=new HashMap<String,Integer>(){
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		{
			put("|",1);
			put("&",2);
			put("!",3);
			put("(",4);
		}
	};
	
	public LogicOperatorNode root;
	
	public LogicSyntaxTree(){
		root=null;
	}
	
	/**
	 * 将中缀表达式转换为逆波兰表达式
	 * @param str
	 * @return
	 */
	private static List<String> reversePolishTransformation(String str){
		str=str.replace("AND", "&");
		str=str.replace("OR", "|");
		str=str.replace("NOT", "!");
		Stack<String> stack=new Stack<String>();
		List<String> list=new ArrayList<String>();
		
		char [] charArr=str.toCharArray();
		int pos=0;
		int i=0;
		for(i=0;i<charArr.length;++i){
			String charStr=charArr[i]+"";
			if(operatorPriority.containsKey(charStr) || ")".equals(charStr)){
				if(i>pos){
					list.add(str.substring(pos, i).trim());
				}
				if("(".equals(charStr)||")".equals(charStr)){
					if("(".equals(charStr)){
						stack.push("(");
					}else{
						while(!stack.peek().equals("(")){
							list.add(stack.pop());
						}
						stack.pop();
					}
				}else{
					if(stack.empty()||operatorPriority.get(stack.peek())<=operatorPriority.get(charStr)){
						stack.push(charStr);
					}else{
						while(!stack.empty()){
							if(operatorPriority.get(stack.peek())>=operatorPriority.get(charStr)&&
									!stack.peek().equals("(")){
								list.add(stack.pop());
							}else if(stack.peek().equals("(")){
								stack.push(charStr);
								break;
							}else{
								break;
							}
						}
					}
				}
				pos=i+1;
			}
		}
		
		if(i>pos){
			list.add(str.substring(pos, i).trim());
		}
		
		while(stack.empty()==false)
			list.add(stack.pop());
		
		return list;
	}
	
	/**
	 * 将逆波兰式转为语法树
	 * @param list
	 * @return
	 */
	private static LogicSyntaxTree reversePolishToSyntaxTree(List<String> list){
		Stack<Object> resStack=new Stack<Object>();
		
		for(int i=0;i<list.size();++i){
			if(operatorPriority.containsKey(list.get(i))){//逻辑运算符
				if(list.get(i).equals("!")){
					LogicOperatorNode node=new LogicOperatorNode();
					node.opt=LOGOPT.NOT;
					if(resStack.peek() instanceof String){
						LogicOperatorNode leaf=new LogicOperatorNode();
						leaf.opt=LOGOPT.LEAF;
						leaf.str=(String)resStack.pop();
						node.leftOperand=leaf;
					}else{
						node.leftOperand=(LogicOperatorNode)resStack.pop();
					}
					resStack.push(node);
				}else{
					LogicOperatorNode node=new LogicOperatorNode();
					if(list.get(i).equals("&")){
						node.opt=LOGOPT.AND;
					}else{// operator '|'
						node.opt=LOGOPT.OR;
					}
					
					//the first operand will be assigned as the right child
					if(resStack.peek() instanceof String){
						LogicOperatorNode leaf=new LogicOperatorNode();
						leaf.opt=LOGOPT.LEAF;
						leaf.str=(String)resStack.pop();
						node.ritghOperand=leaf;
					}else{
						node.ritghOperand=(LogicOperatorNode)resStack.pop();
					}

					//the second operand will be assigned as the left child
					if(resStack.peek() instanceof String){
						LogicOperatorNode leaf=new LogicOperatorNode();
						leaf.opt=LOGOPT.LEAF;
						leaf.str=(String)resStack.pop();
						node.leftOperand=leaf;
					}else{
						node.leftOperand=(LogicOperatorNode)resStack.pop();
					}
					resStack.push(node);
				}
			}else{//操作数
				resStack.push(list.get(i));
			}
		}
		LogicSyntaxTree lsTree=new LogicSyntaxTree();
		if(resStack.peek() instanceof String){
			lsTree.root=new LogicOperatorNode();;
			lsTree.root.opt=LOGOPT.LEAF;
			lsTree.root.str=(String)resStack.pop();
		}else{
			lsTree.root=(LogicOperatorNode)resStack.pop();
		}
		return lsTree;
	}
	
	
	/**
	 * 将中缀逻辑表达式解析成语法树
	 * @param str 中缀逻辑表达式
	 * @return
	 * @throws Exception
	 */
	public static LogicSyntaxTree parse(String str) throws Exception{//(
		int lbCount=leftBracketCount(str);
		int rbCount=rightBracketCount(str);
		
		if(lbCount != rbCount){
			throw new Exception("The number of left bracket is not equal to that of right bracket");
		}
		List<String> list=reversePolishTransformation(str);

		return reversePolishToSyntaxTree(list);
	}
	
	private static int leftBracketCount(String str){
		int pos=-1;
		int count=0;
		while((pos+1<str.length())&&(pos=str.indexOf("(", pos+1))>-1){
			count++;
		}
		return count;
	}
	
	private static int rightBracketCount(String str){
		int pos=-1;
		int count=0;
		while((pos+1<str.length())&&(pos=str.indexOf(")", pos+1))>-1){
			count++;
		}
		return count;
	}
	
	private boolean recursiveContainsVerify(LogicOperatorNode node, List<String> list){
		if(node.opt==LOGOPT.LEAF){
			for(String st:list){
				if(st.indexOf(node.str)>=0)
					return true;
			}
			return false;
		}
		else if(node.opt==LOGOPT.AND){
			return recursiveContainsVerify(node.leftOperand,list) && recursiveContainsVerify(node.ritghOperand,list);
		}else if(node.opt==LOGOPT.OR){
			return recursiveContainsVerify(node.leftOperand,list) || recursiveContainsVerify(node.ritghOperand,list);
		}else{// root.opt==LOGOPT.NOT
			return !recursiveContainsVerify(node.leftOperand,list);
		}
	}
	
	private boolean recursiveContainsVerify(LogicOperatorNode node, String str){
		if(node.opt==LOGOPT.LEAF){
			if(str.indexOf(node.str)>=0)
				return true;
			return false;
		}
		else if(node.opt==LOGOPT.AND){
			return recursiveContainsVerify(node.leftOperand,str) && recursiveContainsVerify(node.ritghOperand,str);
		}else if(node.opt==LOGOPT.OR){
			return recursiveContainsVerify(node.leftOperand,str) || recursiveContainsVerify(node.ritghOperand,str);
		}else{// root.opt==LOGOPT.NOT
			return !recursiveContainsVerify(node.leftOperand,str);
		}
	}
	
	public boolean containsVerify(String str) throws Exception{
		if(this.root==null){
			throw new Exception("node is null");
		}
		return recursiveContainsVerify(this.root,str);
	}
	
	/**
	 * 
	 * @param list 各字段内容
	 * @return
	 * @throws Exception
	 */
	public boolean containsVerify(List<String> list) throws Exception{
		if(this.root==null){
			throw new Exception("node is null");
		}
		return recursiveContainsVerify(this.root,list);
	}
	
	private boolean recursiveExactlyMatch(LogicOperatorNode node, List<String> list){
		if(node.opt==LOGOPT.LEAF){
			for(String st:list){
				if(st.equals(node.str))
					return true;
			}
			return false;
		}
		else if(node.opt==LOGOPT.AND){
			return recursiveExactlyMatch(node.leftOperand,list) && recursiveExactlyMatch(node.ritghOperand,list);
		}else if(node.opt==LOGOPT.OR){
			return recursiveExactlyMatch(node.leftOperand,list) || recursiveExactlyMatch(node.ritghOperand,list);
		}else{// root.opt==LOGOPT.NOT
			return !recursiveExactlyMatch(node.leftOperand,list);
		}
	}
	
	/**
	 * @param list
	 * @return
	 * @throws Exception
	 */
	public boolean exactlyMatch(List<String> list) throws Exception{
		if(this.root==null){
			throw new Exception("root is null");
		}
		return recursiveExactlyMatch(this.root,list);
	}
	
	public static void main(String args[]) throws Exception{
		String str="我是";
		String str1="我是AND好人";
		String str2="(我是|你是)&!好人";
		String sss="(习近平|一带一路)&(习近平|特朗普)&(习近平|普京)&(!(雄安))";
		String [] array=new String[]{
				"习近平AND特朗普",  
				"银行AND(账号OR帐号)", 
				"淘宝ANDNOT手机淘宝",  
				"朝鲜AND(导弹OR核)" 
		};
		
		List<String> list=new ArrayList<String>(){
			{
				//add("我是谁");
				//add("好人");
				add("看过老炮吧，普京就像六爷，即使落魄也不允许别人冒犯，甚至威胁");
			}
		};
		
		LogicSyntaxTree ttt=parse(sss);
		LogicSyntaxTree tree=parse(str);
		LogicSyntaxTree tree1=parse(str1);
		LogicSyntaxTree tree2=parse(str2);
		
		for(String st:array){
			LogicSyntaxTree lst=parse(st);
			System.out.println("fadsf");
		}
		System.out.println(ttt.containsVerify(list));
		
		System.out.println(tree.containsVerify(list));
		System.out.println(tree.exactlyMatch(list));
		
		System.out.println(tree1.containsVerify(list));
		System.out.println(tree1.exactlyMatch(list));
		
		System.out.println(tree2.containsVerify(list));
		System.out.println(tree2.exactlyMatch(list));
	}
}
