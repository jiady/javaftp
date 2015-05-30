package ftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.StringTokenizer;

public class Client {

	public Socket controlSocket,dataSocket;
	public boolean Passive=false;
	public BufferedReader reader;
	public BufferedWriter writer;
	public PrintWriter out;
	public FileInputStream is;
	public BufferedInputStream upload;
	public BufferedReader br;
	public String host;
	public int port = 21;
	public String username;
	public String password;
	public SocketAddress remoteAddr;
	
	
	public Client(String hostname,int p) throws Exception {
		controlSocket = new Socket();
		remoteAddr = new InetSocketAddress(hostname,p);
		host=hostname;
		br = new BufferedReader(new InputStreamReader(System.in));

	}
	
	public BufferedReader getReader(Socket s) throws Exception {
		return new BufferedReader(new InputStreamReader(
				s.getInputStream()));
	}
	
	public void ReadAll(BufferedReader br) throws Exception {
		String msg;
		do{
			msg = br.readLine();
			if(msg!=null)
				System.out.println(msg);
			else 
				break;
		}while(true);
	}
	
	public String readUntil(String st) throws Exception{
		String msg;
		do{
			msg = reader.readLine();
			if(msg!=null)
				System.out.println(msg);
			else 
				break;
		}while(!msg.startsWith(st));
		return msg;
	}
	
	public void safeRead() throws Exception{
		String msg;
		try{
			msg = reader.readLine();
			if(msg!=null)
				System.out.println(msg);
		}catch(java.net.SocketTimeoutException e){
			System.out.println(e.toString());
		}
	}
	public boolean oneshot(String cmd) throws Exception{
		out.println(cmd);
		String msg;
		msg = reader.readLine();
		System.out.println(msg);
		if(msg.startsWith("5")) return false;
		return true;
	}

	public void uploadFile() throws Exception {
		String filename,response;
		System.out.print("Enter Local File Path :");
		filename = br.readLine();
		File f = new File(filename);
		if (!f.exists()) {
			System.out.println("File not Exists...");
			return;
		}
		is = new FileInputStream(f);
		BufferedInputStream input = new BufferedInputStream(is);
		dataSocket = passive();
		oneshot("TYPE I");
		oneshot("STOR " + f.getName());
	
		BufferedOutputStream output = new BufferedOutputStream(
				dataSocket.getOutputStream());
		byte[] buffer = new byte[4096];
		int bytesRead = 0;
		while ((bytesRead = input.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
		output.flush();
		output.close();
		input.close();

		response = reader.readLine();
		System.out.println(response);
	}

	public void download() throws Exception{
		
		String filenameLocal;
		System.out.print("Enter Local Dir Path :");
		filenameLocal = br.readLine();
		File f = new File(filenameLocal);
		
		String filename;
		System.out.print("Enter File Name:");
		filename = br.readLine();
		String response;
	
		dataSocket = passive();
		
		oneshot("TYPE I");
		if(!oneshot("RETR " + filename)){
			return;
		}
		
		BufferedOutputStream output = new BufferedOutputStream(
				new FileOutputStream(new File(filenameLocal, filename)));
		BufferedInputStream input = new BufferedInputStream(
				dataSocket.getInputStream());
		byte[] buffer = new byte[4096];
		int bytesRead = 0;
		while ((bytesRead = input.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
		output.flush();
		output.close();
		input.close();

		response = reader.readLine();
		System.out.println(response);
	}
	
	public void delete() throws Exception{
		String filename ;
		System.out.print("Enter File Path For Delete :");
		filename = br.readLine();
		oneshot("DELE " + filename);
	}
	
	public void makeDir() throws Exception{
		String dirname ;
		System.out.print("Enter Dir Name :");
		dirname = br.readLine();
		oneshot("MKD " + dirname);
	}
	
	
	public void rename() throws Exception{
		String oldName ;
		String newName;
		System.out.print("Enter Old Name :");
		oldName = br.readLine();
		System.out.print("Enter New Name :");
		newName = br.readLine();
		out.println("RNFR " + oldName);
		String msg = reader.readLine();
		System.out.println(msg);
		out.println("RNTO " + newName);
		msg = reader.readLine();
		System.out.println(msg);
	}
	
	public void cwd() throws Exception{
		String path;
		System.out.print("Enter Path:");
		path=br.readLine();
		oneshot("CWD "+path);
	}
	
	public void welcome()throws Exception  {
		readUntil("220 ");
	}
	public void login() throws Exception{
		out.println("USER " + username);
		String response ;
		response = readUntil("331 ");
		//System.out.println(response);
		if (!response.startsWith("331")) {
			throw new IOException(
					"SimpleFTP received an unknown response after sending the user: "
							+ response);
		}

		out.println("PASS " + password);

		response = readUntil("230 ");
		//System.out.println(response);
		if (!response.startsWith("230")) {//230 login success
			throw new IOException(
					"SimpleFTP was unable to log in with the supplied password: "
							+ response);
		}
	}
	
	public Socket passive() throws Exception{
		
		String response ;
		out.println("PASV");
		response = readUntil("227 ");
		if (!response.startsWith("227")) {
			throw new IOException("FTPClient could not request passive mode: "
					+ response);
		}
		String ip = null;
		int port = 0;
		int left = response.indexOf('(');
		int right = response.indexOf(')', left + 1);
		if (right > 0) {
			
			String sub = response.substring(left + 1, right);
			StringTokenizer tokenizer = new StringTokenizer(sub, ",");
			try {
				ip = tokenizer.nextToken() + "." + tokenizer.nextToken() + "."
						+ tokenizer.nextToken() + "." + tokenizer.nextToken();
				port = Integer.parseInt(tokenizer.nextToken()) * 256
						+ Integer.parseInt(tokenizer.nextToken());
			} catch (Exception e) {
				throw new IOException(
						"Format error: "
								+ response);
			}
		}
		return new Socket(ip, port);
	}
	
	public void list() throws Exception{
		dataSocket=passive();
		oneshot("LIST -a -1");
		ReadAll(getReader(dataSocket));
		dataSocket.close();
	}

	public void Menu() throws Exception {
		System.out.println("Username");
		System.out.print("\nEnter Username :");
		String s = br.readLine();
	
		setUsername(s);

		System.out.println("Password");
		System.out.print("\nEnter Password :");
		s = br.readLine();
		
		setPassword(s);
		controlSocket=new Socket();
		controlSocket.connect(remoteAddr,1500);
		reader = new BufferedReader(new InputStreamReader(
				controlSocket.getInputStream()));
		out = new PrintWriter(new OutputStreamWriter(controlSocket.getOutputStream()),
				true);
		//welcome();
		String msg;
		msg = reader.readLine();
		if(msg!=null)
			System.out.println(msg);
		System.out.println("welcome end");
		
		login();
		while (true) {
			
			System.out.println("********************");
			
			System.out.println("1. List all\t*");
			System.out.println("2. Upload File\t*");
			System.out.println("3. Download \t*");
			System.out.println("4. Delete File\t*");
			System.out.println("5. Make Dir\t*");
			System.out.println("6. cwd\t*");
			System.out.println("7. Remane File\t*");
			
			System.out.println("********************");
			System.out.print("Enter Choice :");
			int choice;
			choice = Integer.parseInt(br.readLine());
			
			if(choice == 1){
				list();
			}else if (choice == 2) {
				uploadFile();
			} else if (choice == 3) {
				download();
			} else if (choice == 4) {
				delete();
			} else if (choice == 5) {
				makeDir();
			}else if (choice == 6) {
				cwd();
			}else if(choice ==7){
				rename();
			}
		}
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}


