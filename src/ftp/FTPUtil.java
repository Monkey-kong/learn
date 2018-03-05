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
 * commons-net-3.6ʵ��FTP�ϴ�����
 * 2018-03-03
 */
public class FTPUtil {
    private static String username;
    private static String password;
    private static String ip;
    private static int port;
    private static String configFile;//�����ļ���·����
    private static Properties property=null;//����
    private static FTPClient ftpClient=null;
    private static SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd hh:mm");
    private static final String [] FILE_TYPES={"�ļ�","Ŀ¼","��������","δ֪����"};
    
    public static void main(String[] args) {
    	setConfigFile("E:\\ftp\\lixianfu.properties");//���õ�¼����
    	connectServer();//��¼FTP
    	
    	uploadFile("E:\\ftp\\1.txt", "upload1.txt");//�ϴ��ļ�(�����ص�1.txt�ϴ���FTP������������Ϊupload1.txt)
    	
    	downloadFile("2.txt","E:\\ftp\\download2.txt");//�����ļ�(��FTP������2.txt���ص�����)
    	
    	deleteFile("3.txt");//ɾ���ļ�
    	
    	changeWorkingDirectory("test");//�����ļ���test
    	downloadFile("4.txt","E:\\ftp\\download4.txt");//����test/4.txt
    	
    	downloadBatchFile("/test","E:\\","*.sql");//���ط�����testĿ¼������sql�ļ���E��
    	
    	setFileType(FTP.BINARY_FILE_TYPE);//���ô���������ļ�
    	
    	closeConnect();//�ǳ�FTP
     }
    
    /**
     * ���ӵ�������
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
                System.err.println("��¼ftp��������"+ip+"��ʧ��");
                e.printStackTrace();
            }
        }
    }
    
    /**
      * �ϴ��ļ�
      * @param localFilePath--�����ļ�·��
      * @param newFileName--�µ��ļ���
     */
    public static void uploadFile(String localFullFileName,String newFileName){
        //�ϴ��ļ�
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
      * �����ļ�
      * @param remoteFileName --�������ϵ��ļ���
      * @param localFileName--�����ļ���
     */
    public static void downloadFile(String remoteFileName,String localFullFileName){
        //�����ļ�
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
      * ɾ���ļ�
     */
    public static void deleteFile(String filename){
        try{
             ftpClient.deleteFile(filename);
         }catch(IOException ioe){
             ioe.printStackTrace();
         }
     }
    
    /**
     * ���뵽��������ĳ��Ŀ¼��
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
    * �������ط�����ָ��Ŀ¼�µ�ָ�������ļ���ָ��Ŀ¼
    * @param remoteDirectory --������·��
    * @param localeDirectory--����·��
    * @param regStr --Ҫ���ص��ļ�����
   */
  public static void downloadBatchFile(String remoteDirectory,String localeDirectory,String regStr){
      //�����ļ�
       BufferedOutputStream buffOut=null;
      try{
    	  changeWorkingDirectory(remoteDirectory); //����������·��ת�Ƶ�ָ��·��
    	  FTPFile[] files=ftpClient.listFiles(regStr); //��ȡ·���µ�ָ��·���ļ�
    	  for(FTPFile file:files){//���ļ����ص�ָ���ı���·��
    		  buffOut=new BufferedOutputStream(new FileOutputStream(localeDirectory + file.getName()));
              ftpClient.retrieveFile(file.getName(), buffOut);
              System.out.printf("%-35s%-10s%15s%15s\n","����","����","�޸�����","��С");
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
   * �����ϴ��ļ�
   */
  
	  /**
	   * ���ô����ļ�������[�ı��ļ����߶������ļ�]
	   * @param fileType--BINARY_FILE_TYPE��ASCII_FILE_TYPE 
	  */
	 public static void setFileType(int fileType){
	     try{
	          ftpClient.setFileType(fileType);
	      }catch(Exception e){
	          e.printStackTrace();
	      }
	  }    
  
    /**
     * �ر�����
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
      * ����FTP�ͷ��˵�����--һ����Բ�����
      * @return
     */
    private static FTPClientConfig getFtpConfig(){
         FTPClientConfig ftpConfig=new FTPClientConfig(FTPClientConfig.SYST_NT);
         ftpConfig.setServerLanguageCode(FTP.DEFAULT_CONTROL_ENCODING);
        return ftpConfig;
     }
    
    /**
     * ���������ļ�
     * @param configFile
    */
   public static void setConfigFile(String configFile) {
        FTPUtil.configFile = configFile;
    }
   
   /**
    * ���ò���
    * @param configFile --�����������ļ�
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
	   * ת��[ISO-8859-1 ->   GBK]
	   *��ͬ��ƽ̨��Ҫ��ͬ��ת��
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