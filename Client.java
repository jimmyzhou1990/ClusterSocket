package test.demo3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import com.sun.management.OperatingSystemMXBean;

import test.demo3.Server.SlaverThread;
import test.demo3.Server.TransToWebServerThread;





class SystemManager
{
	private String memory;
	private String threads;
	
    public SystemManager() {
    	new getSystemInfoThread();  //创建获取系统信息的线程
	}
	
	public String getMemory() {
		return memory;
	}
	public void setMemory(String memory) {
		this.memory = memory;
	}
	public String getThreads() {
		return threads;
	}
	public void setThreads(String threads) {
		this.threads = threads;
	}
	
	private void updateMemoryRate()
	{
		int kb = 1024;   
		OperatingSystemMXBean osmxb = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);  
		
		long totalPhysicalMemory = osmxb.getTotalPhysicalMemorySize()/kb; // 总物理内存  
		long freePhysicalMemorySize = osmxb.getFreePhysicalMemorySize() / kb;   
        Double rate = (Double) (1 - freePhysicalMemorySize * 1.0 / totalPhysicalMemory) * 100;  
        String str = rate.intValue() + "%"; 
        
        setMemory(str);
	}
	
	private void updateThreads() {
        // 获得线程总数   
        /*ThreadGroup parentThread;   
        for (parentThread = Thread.currentThread().getThreadGroup(); parentThread.getParent() != null; parentThread = parentThread.getParent());   
        int totalThread = parentThread.activeCount(); 

        setThreads(""+totalThread);*/
		String command = "tasklist";
		int totalThread = 0;
		try {
			Process proc = Runtime.getRuntime().exec(command);
			
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			
			String line;
			
			while ((line=bufferedReader.readLine()) != null)  
			{
				//System.out.println(line);
				totalThread++;
			}
			
			totalThread -= 3;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println("fail to run "+command);
		} 
		
		setThreads(""+totalThread);
	}
	
	class getSystemInfoThread extends Thread{
		public getSystemInfoThread() {
			start();
		}
		
		public void run() {
			while (true)
			{
				updateMemoryRate();
				updateThreads();
				
				try {
					sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}	
}

public class Client {
	
	private String myName;
	
	public Client() throws UnknownHostException
	{
		myName = InetAddress.getLocalHost().toString();
	}
	
	public static void main(String[] args)
	{	
		
		SystemManager systemManager = new SystemManager();
		
		while (true) {
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			try {
				Socket socket = new Socket(args[0], 8888);
				socket.setSoTimeout(1000);

				PrintWriter printWriter = new PrintWriter(socket.getOutputStream(),true);
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				
					
				while(true)
				{
					try {
						//System.out.println("test connect...");
						String tmp = bufferedReader.readLine();

						if (tmp == null) //连接已断开
						{
							System.out.println("Error: lose connect to <" + args[0] + "> !");
							printWriter.close();
					        bufferedReader.close();
					        socket.close();
							break;
						}
					}  
					catch (SocketTimeoutException e) {
						// 连接超时
						System.out.println("connect ok");
						String transToServer = "memory: "+systemManager.getMemory() + " threads: " + systemManager.getThreads();
						
						System.out.println(transToServer);
						printWriter.println(transToServer);
					}
					catch (IOException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						System.out.println("Error: lose connect to <" + args[0] + "> !");
						printWriter.close();
				        bufferedReader.close();
				        socket.close();
				        break;
					}		
				}
				
			} 
			
			catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println("Error: Cannot setup connect to <" + args[0] + "> !");
			}
			
			
			
		}
		
	}
}
