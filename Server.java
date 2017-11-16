package test.demo3;

import java.beans.DefaultPersistenceDelegate;
import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

import net.sf.json.JSONArray;
import sun.security.x509.DeltaCRLIndicatorExtension;

import java.util.Map.Entry;





public class Server {
	
	private static ClientManager clientManager = null;
	private static InetAddress LocalhostAddr = null;
	
	//获取本机ip方法
	private static InetAddress getLocalhostAddr() {
		InetAddress bindAddr;
		try {
			bindAddr = InetAddress.getLocalHost();
			return bindAddr;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("Error! cannot get localhost ip!");
			return null;
		}
		
	}
	
	//转化IP方法
	public String transformIpAddr(String internetIp) {
		
		return internetIp.substring(1);
		
	}
	
	//Server构造方法
	public Server(int port, int backlog, InetAddress bindAddr, String webserveraddr) throws IOException {
		
		clientManager = new ClientManager();
		new ConsoleThread();
		new SlaverThread(webserveraddr);  //创建线程作为web服务器的客户端
		
		//创建server服务器
		ServerSocket serverSocket = new ServerSocket(port, backlog, bindAddr);
		
		while(true)
		{
			//开始监听
			Socket socket = serverSocket.accept();
			String client = socket.getInetAddress().toString();
			client = transformIpAddr(client);
			
			//如果client正在被管理则创建线程,否则拒绝连接
			if (clientManager.isClientAdded(client))
			{
				//设置为online
				clientManager.setClientConnect(client, "online");
				new MasterThread(socket);
			}
			else
			{
				System.out.println("<" + client + "> is not added, refuse to connect!");
				socket.close();
			}	
		}
	}
	
	//命令输入线程类
	private class ConsoleThread extends Thread{
		
		public ConsoleThread() {
			start();
		}
		
		public void run() {
			
			while(true)
			{
				BufferedReader sysBuff = new BufferedReader(new InputStreamReader(System.in));
				String cmd;
				try {
					cmd = sysBuff.readLine();
					switch (cmd) {
					case "clients":
						clientManager.print();
						break;

					default:
						break;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					System.out.println(getName() + ": Wrong input, try again");
					System.out.println("clients--query all the clients online!");
				}
			}
		}
	}
	
	
	//webserver命令处理类
	public class WebserverCommand
	{
		private String commandLine = null;
		
		private static final String cmdSET = "set";
		private static final String cmdADD = "add";
		private static final String cmdQUERYSC = "sc";
		private static final String objSocketServerName = "ss";
		private static final String objSocketClientName = "sc";
		
		public WebserverCommand(String cmd) {
			commandLine = cmd;
		}
		
		public void setCommand(String line) {
			commandLine = line;
		}
		
		public void parse(Socket socket) {
			String[] command = {"cmd", "object", "value"};
			int i = 0;
			StringTokenizer st = new StringTokenizer(commandLine, " "); 
			
	        while(st.hasMoreElements()) {  
	        	command[i++] = st.nextElement().toString();
	        	if (i >= 3)    break;  //只接受3个单词
	        } 
	        
			switch (command[0])
			{
				case  cmdADD:
					switch (command[1]) 
					{
						case objSocketClientName:
							clientManager.addClient(command[2]);
							break;

						default:
							break;
					}
					break;
				
				case cmdQUERYSC:
					switch (command[1])
					{
						case "???":
							//发送Clients状态
							new TransToWebServerThread(socket, clientManager.toJsonString());
							break;
					}
					break;
			}
		}
	}
	
	public class ClientBean {
		private String ip;
		private String connect;
		private String memory;
		
		
		public String getIp() {
			return ip;
		}
		public void setIp(String ip) {
			this.ip = ip;
		}
		public String getConnect() {
			return connect;
		}
		public void setConnect(String connect) {
			this.connect = connect;
		}
		public String getMemory() {
			return memory;
		}
		public void setMemory(String memory) {
			this.memory = memory;
		}
	}
	
	//客户端管理类
	private class ClientManager
	{
		final static String ONLINE = "online";
		final static String OFFLINE = "offline";
		
		public LinkedList<ClientBean> Clients = new LinkedList<ClientBean>();
		
		//判断客户端是否被管理
		public synchronized boolean isClientAdded(String newIP)
		{
			//ip是否已存在
			String addedIP;
			
			for(Iterator<ClientBean> iter = Clients.iterator(); iter.hasNext();)
			{
				addedIP = ((ClientBean)iter.next()).getIp();
				
				System.out.println(addedIP + "    vs   " + newIP);
				if (newIP.equals(addedIP))
				{
					System.out.println("The ip <" + newIP + "> is exist!");
					return true;
				}
			}
			
			return false;	
		}
		
		//返回客户端在链表中的位置
		private int getPosition(String clientIP) {
			
			int position = 0;
			String addedIP;
			
			for(Iterator<ClientBean> iter = Clients.iterator(); iter.hasNext();)
			{
				addedIP = ((ClientBean)iter.next()).getIp();
				if (addedIP.equals(clientIP))
				{
					System.out.println("The ip <" + clientIP + "> is exist!");
					return position;
				}
				position++;
			}
			
			return -1;
		}
		
		//添加客户端,添加时设为OFFLINE,不能重复添加
		public synchronized boolean addClient(String clientIP)
		{
			if (isClientAdded(clientIP))
			{
				return false;
			}
			else
			{
				ClientBean c = new ClientBean();
				System.out.println("add client <" + clientIP + ">");
				c.setIp(clientIP);
				Clients.add(c);
				return true;
			}
		}
		
		//删除客户端,仅不再管理客户端,连接可能仍然存在
		public synchronized void removeClient(String clientIP)
		{
			int position = getPosition(clientIP);
			
			if (position >= 0)
			{
				Clients.remove(position);
			}
		}
		
		//若客户端正在被管理,则设置其状态
		public synchronized void setClientConnect(String clientIP, String status)
		{
			int position = getPosition(clientIP);
			
			System.out.println("<"+clientIP+"> position: " + position);
			
			if (position >= 0)
			{
				ClientBean c = Clients.get(position);
				
				c.setConnect(status);
				
				Clients.set(position, c);
			}
		}	
		
		public synchronized String getConnectStatus(String clientIP)
		{
			
			return null;
		}
		
		public synchronized void print() 
		{
			ClientBean c;
			
			for(Iterator<ClientBean> iter = Clients.iterator(); iter.hasNext();)
			{
				c = (ClientBean)iter.next();
				System.out.println("<"+c.getIp()+">    "+"con: "+c.getConnect()+"    mem: "+c.getMemory());
			}
			
			System.out.println("Json String:");
			System.out.println(toJsonString());
		}
		
		//转化为Json字符串
		public String toJsonString() {
			String jsonString;
			
			jsonString = JSONArray.fromObject(Clients).toString();
			
			return jsonString;
		}
	}
	
	//发送至webserver线程
	public class TransToWebServerThread extends Thread
	{
		private Socket socket;
		private String transBuf;
		public TransToWebServerThread(Socket socket, String transBuf) {
			super();
			this.socket = socket;
			this.transBuf = transBuf;
			
			start();
		}
		
		public void run()
		{
			try {
				System.out.println("Send \"" + transBuf + "\"");
				PrintWriter printWriter = new PrintWriter(socket.getOutputStream(),true);
				printWriter.println(transBuf);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Send \"" + transBuf + "\" fail!");
			}
		}
	}
	
	//master线程
	private class MasterThread extends Thread{
		private Socket connect;
		private BufferedReader bufferedReader;
		private PrintWriter printWriter;
		private String ClientName;
		
		public MasterThread(Socket s) throws IOException
		{
			connect = s;
			bufferedReader = new BufferedReader(new InputStreamReader(connect.getInputStream()));	
			printWriter = new PrintWriter(connect.getOutputStream(), true);
			ClientName = connect.getInetAddress().toString();
			ClientName = transformIpAddr(ClientName);
			
			System.out.println(ClientName + " connected!");
			
			clientManager.setClientConnect(ClientName, "online");
			
			start();
		}
		
		public void destroy() {
			
			//设置为offline
			clientManager.setClientConnect(ClientName, "offline");
			
			printWriter.close();
			
			try {
				bufferedReader.close();
				connect.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Error when destroy connect <" + ClientName + "> !!!");
			}
		}
		
		public void run() 
		{
			String line;
			try {
				line = bufferedReader.readLine();
				while (!line.equals("bye"))
				{
					printWriter.println("continue, Client(" + getName() +")!");
					line = bufferedReader.readLine();
					
					//System.out.println(ClientName + " say: " + line);
				}
				
				printWriter.println("bye, Client(" + getName() +")!");
				System.out.println(ClientName + " exit!");
				
				destroy();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("lose connect to client <" + ClientName + "> !!!");
				destroy();
			}
			
			System.out.println("Leave thread [" + getName() + "]...");
		}
	
	}
	
	//Slaver线程
	class SlaverThread extends Thread
	{
		private String WebServerAddr = null;
		
		public SlaverThread(String webserverip) {
			WebServerAddr = webserverip;
			start();
		}
		
		public void run() {
			
			while (true) {
				
					try {
						Socket socket = new Socket(WebServerAddr, 8887);
						socket.setSoTimeout(2000);
						
						BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						String result ="";
						WebserverCommand command = new WebserverCommand(result);
						while(true)
						{
							try {
								result = bufferedReader.readLine();
								System.out.println("WebServer say : " + result);
								
								command.setCommand(result);
								command.parse(socket);
							}  
							catch (SocketTimeoutException e) {
								// 连接超时
								System.out.println("read form webserver <" + WebServerAddr + "> time out!");
								
								//发送clients状态
								new TransToWebServerThread(socket, clientManager.toJsonString());
							}
							catch (IOException e) {
								// TODO Auto-generated catch block
								//e.printStackTrace();
								System.out.println("IOException: read from webserver!");
								break;
							}
							
						}
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						//e1.printStackTrace();
						System.out.println("Cannot connect to <" + WebServerAddr + ">! ");
						System.out.println("  1. The client is added into management ?");
						System.out.println("  2. The the server ip is right?");
						System.out.println("  3. The the server is online?");
					}
 	
			}
		}
	}

	public static void main(String[] args) throws IOException{
		
		LocalhostAddr = getLocalhostAddr();
		if (LocalhostAddr != null)
		{
			new Server(8888, 30, LocalhostAddr, args[0]);        //作为socket服务器
		}
		
		System.out.println("main() Exit!");
	}	
}


