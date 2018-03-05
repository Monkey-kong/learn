package ftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * commons-net-3.6实现FTP上传下载
 * 2018-03-03
 */
public class FTPUtil {
    private static String username;
    private static String password;
    private static String ip;
    private static int port;
    private static String configFile;//配置文件的路径名
    private static Properties property=null;//配置
    private static FTPClient ftpClient=null;
    private static SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd hh:mm");
    private static final String [] FILE_TYPES={"文件","目录","符号链接","未知类型"};
    
    public static void main(String[] args) {
    	setConfigFile("E:\\ftp\\lixianfu.properties");//设置登录参数
    	connectServer();//登录FTP
    	
    	uploadFile("E:\\ftp\\1.txt", "upload1.txt");//上传文件(将本地的1.txt上传到FTP服务器，命名为upload1.txt)
    	
    	downloadFile("2.txt","E:\\ftp\\download2.txt");//下载文件(将FTP服务器2.txt下载到本地)
    	
    	deleteFile("3.txt");//删除文件
    	
    	changeWorkingDirectory("test");//进入文件夹test
    	downloadFile("4.txt","E:\\ftp\\download4.txt");//下载test/4.txt
    	
    	downloadBatchFile("/test","E:\\","*.sql");//下载服务器test目录下所有sql文件到E盘
    	
    	setFileType(FTP.BINARY_FILE_TYPE);//设置传输二进制文件
    	
    	closeConnect();//登出FTP
     }
    
    /**
     * 连接到服务器
    */
   public static void connectServer() {
       if (ftpClient == null) {
           int reply;
           try {
           	 setArg(configFile);
                ftpClient=new FTPClient();
                ftpClient.setDefaultPort(port);
                ftpClient.configure(getFtpConfig());
                ftpClient.connect(ip);
                ftpClient.login(username, password);
                ftpClient.setDefaultPort(port);
                System.out.print(ftpClient.getReplyString());
                reply = ftpClient.getReplyCode();

               if (!FTPReply.isPositiveCompletion(reply)) {
                    ftpClient.disconnect();
                    System.err.println("FTP server refused connection.");
                }
            } catch (Exception e) {
                System.err.println("登录ftp服务器【"+ip+"】失败");
                e.printStackTrace();
            }
        }
    }
    
    /**
      * 上传文件
      * @param localFilePath--本地文件路径
      * @param newFileName--新的文件名
     */
    public static void uploadFile(String localFullFileName,String newFileName){
        //上传文件
         BufferedInputStream buffIn=null;
        try{
             buffIn=new BufferedInputStream(new FileInputStream(localFullFileName));
             ftpClient.storeFile(newFileName, buffIn);
         }catch(Exception e){
             e.printStackTrace();
         }finally{
            try{
                if(buffIn!=null)
                     buffIn.close();
             }catch(Exception e){
                 e.printStackTrace();
             }
         }
     }
    
    /**
      * 下载文件
      * @param remoteFileName --服务器上的文件名
      * @param localFileName--本地文件名
     */
    public static void downloadFile(String remoteFileName,String localFullFileName){
        //下载文件
         BufferedOutputStream buffOut=null;
        try{
             buffOut=new BufferedOutputStream(new FileOutputStream(localFullFileName));
             ftpClient.retrieveFile(remoteFileName, buffOut);
         }catch(Exception e){
             e.printStackTrace();
         }finally{
            try{
                if(buffOut!=null)
                     buffOut.close();
             }catch(Exception e){
                 e.printStackTrace();
             }
         }
     }
    
    /**
      * 删除文件
     */
    public static void deleteFile(String filename){
        try{
             ftpClient.deleteFile(filename);
         }catch(IOException ioe){
             ioe.printStackTrace();
         }
     }
    
    /**
     * 进入到服务器的某个目录下
     * @param directory
    */
   public static void changeWorkingDirectory(String directory){
       try{
            connectServer();
            ftpClient.changeWorkingDirectory(directory);
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }
    
   /**
    * 批量下载服务器指定目录下的指定类型文件到指定目录
    * @param remoteDirectory --服务器路径
    * @param localeDirectory--本地路径
    * @param regStr --要下载的文件类型
   */
  public static void downloadBatchFile(String remoteDirectory,String localeDirectory,String regStr){
      //下载文件
       BufferedOutputStream buffOut=null;
      try{
    	  changeWorkingDirectory(remoteDirectory); //服务器工作路径转移到指定路径
    	  FTPFile[] files=ftpClient.listFiles(regStr); //获取路径下的指定路径文件
    	  for(FTPFile file:files){//将文件下载到指定的本地路径
    		  buffOut=new BufferedOutputStream(new FileOutputStream(localeDirectory + file.getName()));
              ftpClient.retrieveFile(file.getName(), buffOut);
              System.out.printf("%-35s%-10s%15s%15s\n","名称","类型","修改日期","大小");
              System.out.printf("%-35s%-10s%15s%15s\n",iso8859togbk(file.getName()),FILE_TYPES[file.getType()]
                      ,dateFormat.format(file.getTimestamp().getTime()),FileUtils.byteCountToDisplaySize(file.getSize()));
              buffOut.flush();
    	  }
       }catch(Exception e){
           e.printStackTrace();
       }finally{
          try{
              if(buffOut!=null)
                   buffOut.close();
           }catch(Exception e){
               e.printStackTrace();
           }
       }
   }

   /**
   * 批量上传文件
   */
  
	  /**
	   * 设置传输文件的类型[文本文件或者二进制文件]
	   * @param fileType--BINARY_FILE_TYPE、ASCII_FILE_TYPE 
	  */
	 public static void setFileType(int fileType){
	     try{
	          ftpClient.setFileType(fileType);
	      }catch(Exception e){
	          e.printStackTrace();
	      }
	  }    
  
    /**
     * 关闭连接
    */
   public static void closeConnect(){
       try{
           if(ftpClient!=null){
                ftpClient.logout();
                ftpClient.disconnect();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    /**
      * 设置FTP客服端的配置--一般可以不设置
      * @return
     */
    private static FTPClientConfig getFtpConfig(){
         FTPClientConfig ftpConfig=new FTPClientConfig(FTPClientConfig.SYST_NT);
         ftpConfig.setServerLanguageCode(FTP.DEFAULT_CONTROL_ENCODING);
        return ftpConfig;
     }
    
    /**
     * 设置配置文件
     * @param configFile
    */
   public static void setConfigFile(String configFile) {
        FTPUtil.configFile = configFile;
    }
   
   /**
    * 设置参数
    * @param configFile --参数的配置文件
   */
  private static void setArg(String configFile){
       property=new Properties();
       BufferedInputStream inBuff=null;
      try{
           inBuff=new BufferedInputStream(new FileInputStream(configFile));
           property.load(inBuff);
           username=property.getProperty("username");
           password=property.getProperty("password");
           ip=property.getProperty("ip");
           port=Integer.parseInt(property.getProperty("port"));
       }catch(Exception e){
           e.printStackTrace();
       }finally{
          try{
              if(inBuff!=null)
                   inBuff.close();
           }catch(Exception e){
               e.printStackTrace();
           }
       }
   }
  
	  /**
	   * 转码[ISO-8859-1 ->   GBK]
	   *不同的平台需要不同的转码
	   * @param obj
	   * @return
	  */
	 private static String iso8859togbk(Object obj){
	     try{
	         if(obj==null)
	             return "";
	         else
	             return new String(obj.toString().getBytes("iso-8859-1"),"GBK");
	      }catch(Exception e){
	         return "";
	      }
	  }
  
    
}