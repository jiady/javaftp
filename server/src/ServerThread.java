import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Stack;


class ServerThread extends Thread  
    {  
  
        Socket sk = null;  
        BufferedReader in = null;  
        PrintWriter out = null;  
        String username=null,password=null;
        boolean auth;
        String pathfix;
        Stack<String> path;
        
        private ServerSocket serverSocket=null;
        InetAddress host;
        final static int backlog=5;
        
        public ServerThread(Socket sk)  
        {  
            this.sk = sk;  
        }  
        public void run()  
        {  
            try  
            {  
            	pathfix="./data";
            	path=new Stack<String>();
                out = new PrintWriter(new OutputStreamWriter(sk.getOutputStream()),
        				true);  
                in = new BufferedReader(new InputStreamReader(sk  
                        .getInputStream())); 
                out.println("220 welcome to dongyu's diy server");
                
                String line;
                System.out.println("220 welcome to dongyu's diy server");
                while((line = in.readLine())!=null){
                	System.out.println(line);
                	 String [] arg=line.split(" ");
//                   特别，下面这句得加上     “\n”,  
                	 switch (arg[0]){
                	 	case "USER":
                	 		user(arg);
                	 						break;
                	 	case "PASS":
                	 		pass(arg);
                	 						break;
                	 	case "PASV":
                	 		pasv();
                	 						break;
                	 	case "TYPE":
                	 		type(arg);
                	 						break;
                	 	case "STOR"://upload
                	 		upload(arg);
                	 						break;
                	 	case "LIST":
                	 		list(arg);
                	 						break;
                	 	case "RETR"://download;
                	 		retr(arg);
                	 						break;
                	 	case "DELE":
                	 		dele(arg);
                	 						break;
                	 	case "CWD":
                	 		cd(arg);
                	 						break;
                	 	case "MKD":
                	 		mkdir(arg);
                	 						break;
                	 	default:
                	 		out.println("202 Command not implemented, superfluous at this site.");
                	 		break;
                	 
                	 }
                     out.flush();
                     System.out.println("one cmd has been finished");
                }
            }  
            catch (IOException e)  
            {  
                e.printStackTrace();  
            }  
              
        } 
        public void type(String [] args){
        	if(args.length>1){
        		String t=args[1];
        		out.println("200 type changed");
        	}else{
        		out.println("500 Syntax error, command unrecognized.");
        	}
        }
       
        public void user(String [] args){
        	if(args.length>1){
        		username=args[1];
        		out.println("331 User name okay, need password");
        	}else{
        		out.println("500 Syntax error, command unrecognized.");
        	}
        }
        public void pass(String [] args){
        	if(args.length>1){
        		password=args[1];
        		auth=true;
        		out.println("230 User logged in, proceed.");
        	}else{
        		out.println("500 Syntax error, command unrecognized.");
        	}
        }
        public String pwd() 
    	{
    		// List the current working directory.
    		String p = "/";
    		for (String e:path)
    		{
    			p += e + "/";
    		}
    		return p;
    	}

    	private String getpath()
    	{
    		return pathfix + pwd();
    	}
    	private boolean valid(String s)
    	{
    		// File names should not contain "/".
    		return (s.indexOf('/') < 0);
    	}
        public void cd(String [] args){
        	if(args.length>1){
        		String dir=args[1];
        		if (!valid(dir))
        		{
        			out.println("451 move only one level at one time.'/' is not allowed");
        		}
        		else
        		{
        			if ("..".equals(dir))
        			{
        				if (path.size() > 0)
        					path.pop();
        				else{
        					out.println("451 Requested action aborted: already in root dir.");
        					return;
        				}
        			}
        			else if (".".equals(dir))
        			{
        				;
        			}
        			else
        			{
        				File f = new File(getpath());
        				if (!f.exists()){
        					out.println("451 Requested action aborted: Directory does not exist: " + dir);
        					return;
        				}
        				else if (!f.isDirectory()){
        					out.println("451 Requested action aborted:Not a directory: " + dir);
        					return;
        				}
        				else{
        					path.push(dir);
        				}
        			}
        			out.println("250 dir switched.");
        		}

        	}else{
        		out.println("500 Syntax error, command unrecognized.");
        	}
        }
        public void mkdir(String [] args){
        	if(args.length>1){
        		String destDirName=getpath()+args[1];
        		File dir = new File(destDirName);
    		    if(dir.exists()) {
    		    	System.out.println("创建目录" + destDirName + "失败，目标目录已存在！");
    		    	out.println("451 Requested dir exists.");
    		    	return;
    		    }
    		    if(!destDirName.endsWith(File.separator))
    		    	destDirName = destDirName + File.separator;
    		    // 创建单个目录
    		    if(dir.mkdirs()) {
	    		     System.out.println("创建目录" + destDirName + "成功！");
	    		     out.println("250 Requested file action okay, completed.");
    		    }else{
    		    	out.println("500 something wrong.");
    		    }
        	}else{
        		out.println("500 Syntax error, command unrecognized.");
        	}
        }
        public void dele(String [] args){
        	if(args.length>1){
        		String destDirName=getpath()+args[1];
        		File dir = new File(destDirName);
    		    if(dir.exists()) {
    		    	if(!dir.isDirectory()){
	    		    	//file
    		    		dir.delete();
	    		    	out.println("250 file delete");
	    		    	return;
    		    	}else{
    		    		delFolder(destDirName);
    		    		out.println("250 folder delete");
	    		    	return;
    		    	}
    		    }
    		    else{
    		    	out.println("451 no such file or folder");
    		    }
        	}else{
        		out.println("500 Syntax error, command unrecognized.");
        	}
        }
        
        public void pasv() throws IOException{
        	serverSocket = new ServerSocket(0, backlog, host);
        	InetSocketAddress h=(InetSocketAddress) (serverSocket.getLocalSocketAddress());
        	int p=h.getPort();
        	int p1=p%256;
        	int p0=p/256;
        	byte b[]=h.getAddress().getAddress();
        	System.out.printf("227 (%d,%d,%d,%d,%d,%d)\n",b[0],b[1],b[2],b[3],p0,p1);
        	out.printf("227 (%d,%d,%d,%d,%d,%d)\n",b[0],b[1],b[2],b[3],p0,p1);
        }
        
        public void list(String [] args) {
        	String [] lst= new File(getpath()).list();
        	out.println("200 processing");
        	String send="";
        	for(String t:lst){
        		send=send.concat(t).concat("\n");
        	}
        	send=send.concat(".\n");
        	send=send.concat("..\n");
        	ByteArrayInputStream is = new ByteArrayInputStream(send.getBytes());
        	new Thread(new GetThread(serverSocket, is,out)).start();
        }
        public void retr(String [] args) {
        	if(args.length>1){
        		String destDirName=getpath()+args[1];
        		FileInputStream f;
				try {
					f = new FileInputStream(destDirName);
					new Thread(new GetThread(serverSocket, f,out)).start();
					out.println("200 processing");
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					out.println("451 no such file or folder");
				}
    			
        	}else{
        		out.println("500 Syntax error, command unrecognized.");
        	}
        }
        public void upload(String [] args){
        	if(args.length>1){
        		String destDirName=getpath()+args[1];
				FileOutputStream f;
				try {
					f = new FileOutputStream(destDirName);
					new Thread(new PutThread(serverSocket, f,out)).start();
					out.println("200 processing");
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					out.println("451 no such file or folder");
				}
				
        	}else{
        		out.println("500 Syntax error, command unrecognized.");
        	}
        }
        private static class GetThread implements Runnable//download
    	{
    		private ServerSocket dataChan = null;
    		private InputStream file = null;
    		PrintWriter pt;

    		public GetThread(ServerSocket s, InputStream f,PrintWriter out)
    		{
    			dataChan = s;
    			file =  f;
    			pt=out;
    		}
    		public void run()
    		{
    			try
    			{
    				//System.out.println(" wait for the client's initial socket");
    				Socket xfer = dataChan.accept();
    				//System.out.println(" Prepare the output to the socket");
    				BufferedOutputStream out = new BufferedOutputStream(
    						xfer.getOutputStream());
    				System.out.println(file.toString());
    				// read the file from disk and write it to the socket
    				byte[] sendBytes = new byte[4096];
    				int iLen = 0;
    				while ((iLen = file.read(sendBytes)) != -1)
    				{
    					out.write(sendBytes, 0, iLen);
    				}
    				out.flush();
    				out.close();
    				xfer.close();
    				file.close();
    				System.out.println("get over");
    			}
    			catch (Exception e)
    			{
    				e.printStackTrace();
    			}
    			pt.println("200 trasfer complete");
    		}
    	}
        private static class PutThread implements Runnable
    	{
    		private ServerSocket dataChan = null;
    		private FileOutputStream file = null;
    		PrintWriter pt;
    		
    		public PutThread(ServerSocket s, FileOutputStream f,PrintWriter out)
    		{
    			dataChan = s;
    			file = f;
    			pt=out;
    		}

    		public void run()
    		{
    			/*
    			 * TODO: Process a client request to transfer a file.
    			 */
    			try
    			{
    				// wait for the client's initial socket
    				Socket xfer = dataChan.accept();

    				// Prepare the input from the socket
    				BufferedInputStream in = new BufferedInputStream(
    						xfer.getInputStream());

    				// read the data from the socket and write to the disk file
    				byte[] inputByte = new byte[4096];
    				int iLen = 0;
    				while ((iLen = in.read(inputByte)) != -1)
    				{
    					file.write(inputByte, 0, iLen);
    					
    				}
    				file.flush();
    				in.close();
    				xfer.close();
    				file.close();
    			}
    			catch (Exception e)
    			{
    				e.printStackTrace();
    			}
    			
    			pt.println("200 trasfer complete");
    		}
    	}

        public void delAllFile(String path) { 
            File file = new File(path); 
            if (!file.exists()) { 
                return; 
            } 
            if (!file.isDirectory()) { 
                return; 
            } 
            String[] tempList = file.list(); 
            File temp = null; 
            for (int i = 0; i < tempList.length; i++) { 
                if (path.endsWith(File.separator)) { 
                    temp = new File(path + tempList[i]); 
                } 
                else { 
                    temp = new File(path + File.separator + tempList[i]); 
                } 
                if (temp.isFile()) { 
                    temp.delete(); 
                } 
                if (temp.isDirectory()) { 
                    delAllFile(path+"/"+ tempList[i]);//先删除文件夹里面的文件 
                    delFolder(path+"/"+ tempList[i]);//再删除空文件夹 
                } 
            } 
        }
        public void delFolder(String folderPath) { 
            try { 
                delAllFile(folderPath); //删除完里面所有内容 
                String filePath = folderPath; 
                filePath = filePath.toString(); 
                java.io.File myFilePath = new java.io.File(filePath); 
                myFilePath.delete(); //删除空文件夹 
            } 
            catch (Exception e) { 
                System.out.println("删除文件夹操作出错"); 
                e.printStackTrace(); 
            } 
        } 
        
    }  