package test.demo3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.sun.security.sasl.ServerFactoryImpl;
import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

public class Client {
	
	private String myName;
	
	public Client() throws UnknownHostException
	{
		myName = InetAddress.getLocalHost().toString();
	}
	
	public static void main(String[] args)
	{	
		while (true) {
			
			try {
				Socket socket = new Socket(args[0], 8888);
				socket.setSoTimeout(60000);
				
				PrintWriter printWriter = new PrintWriter(socket.getOutputStream(),true);
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				
				String result ="";
				
				while(result.indexOf("bye") == -1)
				{
					//BufferedReader consoleBuff = new BufferedReader(new InputStreamReader(System.in));
					//printWriter.println(consoleBuff.readLine());
					
					result = bufferedReader.readLine();
					System.out.println("Server say : " + result);
				}
				
				System.out.println("关闭连接");
				
				printWriter.close();
		        bufferedReader.close();
		        socket.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println("Error: Cannot connect to <" + args[0] + "> ! Please check:");
				System.out.println("  1. The client is added into management ?");
				System.out.println("  2. The the server ip is right?");
				System.out.println("  3. The the server is online?");
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("fail to sleep(1000)");
			}
			
		}
		
	}
}
