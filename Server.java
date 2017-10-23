package test.demo3;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.omg.CORBA.PUBLIC_MEMBER;

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
	
	//Server构造方法
	public Server(int port, int backlog, InetAddress bindAddr) throws IOException {
		
		clientManager = new ClientManager();
		new SlaverThread();  //创建线程作为web服务器的客户端
		
		//创建server服务器
		ServerSocket serverSocket = new ServerSocket(port, backlog, bindAddr);
		
		while(true)
		{
			//开始监听
			Socket socket = serverSocket.accept();
			String client = socket.getInetAddress().toString();
			
			//如果client正在被管理则创建线程,否则拒绝连接
			if (clientManager.isClientAdded(client))
			{
				//设置为online
				clientManager.setClientOnline(client);
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
	
	//客户端管理类
	private class ClientManager
	{
		final static int ONLINE = 1;
		final static int OFFLINE = 1;
		
		private HashMap<String, Integer> Clients = new HashMap<String, Integer>(); 
		
		//判断客户端是否被管理
		public synchronized boolean isClientAdded(String client)
		{
			return Clients.containsKey(client);
		}
		
		//添加客户端,添加时设为OFFLINE,不能重复添加
		public synchronized boolean addClient(String client)
		{
			if (Clients.get(client) == null)
			{
				Clients.put(client, OFFLINE);
				return true;
			}
			else
			{
				System.out.println("the " + client + " is already added!");
				return false;
			}
		}
		
		//删除客户端,仅不再管理客户端,连接可能仍然存在
		public synchronized void removeClient(String client)
		{
			Clients.remove(client);
		}
		
		//若客户端正在被管理,则设置其状态为ONLINE
		public synchronized void setClientOnline(String client)
		{
			if (Clients.containsKey(client))
			{
				//注意put既能修改也能添加,所以先判断key是否存在
				Clients.put(client, ONLINE); 
			}
		}	
		
		//若客户端正在被管理,则设置其状态为OFFLINE
		public synchronized void setClientOffline(String client)
		{
			if (Clients.containsKey(client))
			{
				//注意put既能修改也能添加,所以先判断key是否存在
				Clients.put(client, OFFLINE); 
			}
		}
		
		public synchronized boolean isOnline(String client)
		{
			if (Clients.get(client) == null)
			{
				return false;
			}
			else
			{
				return true;
			}
		}
		
		public synchronized void print() {
	        Iterator iter = Clients.entrySet().iterator(); 
	        
	        if(!iter.hasNext())
	        {
	        	System.out.println("No clients online");  
	        }

	        while (iter.hasNext()) {  
	            Map.Entry entry = (Map.Entry)iter.next();  
	            Object key = entry.getKey();  
	            Object value = entry.getValue(); 
	            
	            System.out.println(key + "  :  " + (value.equals(1)?"online":"offline"));  
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
			
			System.out.println(ClientName + " connected!");
			
			start();
		}
		
		public void destroy() {
			
			//设置为offline
			clientManager.setClientOffline(ClientName);
			
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
				System.out.println("lose connect to <" + ClientName + "> !!!");
				destroy();
			}
		}
	
	}
	
	//Slaver线程
	class SlaverThread extends Thread
	{
		public void ClientThread() {
			
			start();
		}
		
		public void run() {
			
		}
	}

	public static void main(String[] args) throws IOException{
		
		LocalhostAddr = getLocalhostAddr();
		if (LocalhostAddr != null)
		{
			new Server(8888, 30, LocalhostAddr);        //作为socket服务器
		}
		
		System.out.println("main() Exit!");
	}	
}


