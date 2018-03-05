package sftp;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
 * jsch-0.1.54ʵ��sftp�ϴ�����
 * 
 * @author lixianfu
 * @date 2018-03-01
 * @time ����1:39:44
 * @version 1.0
 */
public class SFTPUtils
{
  private static final Log LOG = LogFactory.getLog(SFTPUtils.class);
 
  private String host;//����������ip
  private String username;//�û���
  private String password;//����
  private int port = 22;//�˿ں�
  private ChannelSftp sftp = null;
  private Session sshSession = null;
 
  public SFTPUtils(){}
 
  public SFTPUtils(String host, int port, String username, String password)
  {
    this.host = host;
    this.username = username;
    this.password = password;
    this.port = port;
  }
 
  public SFTPUtils(String host, String username, String password)
  {
    this.host = host;
    this.username = username;
    this.password = password;
  }
 
  /**
   * ͨ��SFTP���ӷ�����
 * @throws Exception 
   */
  public ChannelSftp connect() throws Exception
  {
    try
    {
      JSch jsch = new JSch();
      jsch.getSession(username, host, port);
      sshSession = jsch.getSession(username, host, port);
      LOG.info("Session created.");
      sshSession.setPassword(password);
      Properties sshConfig = new Properties();
      sshConfig.put("StrictHostKeyChecking", "no");
      sshSession.setConfig(sshConfig);
      sshSession.connect();
      LOG.info("Session connected.");
      Channel channel = sshSession.openChannel("sftp");
      channel.connect();
      LOG.info("Opening Channel.");
      sftp = (ChannelSftp) channel;
      LOG.info("Connected to " + host + ".");
      return sftp;
    }
    catch (Exception e)
    {
    	throw new Exception("����SFTP��" + host + "������", e);
    }
  }
 
  /**
   * �ر�����
 * @throws Exception 
   */
  public void disconnect() throws Exception
  {
	try
	{
	    if (this.sftp != null)
	    {
	      if (this.sftp.isConnected())
	      {
	        this.sftp.disconnect();
	        LOG.info("sftp is closed already");
	      }
	    }
	    if (this.sshSession != null)
	    {
	      if (this.sshSession.isConnected())
	      {
	        this.sshSession.disconnect();
	        LOG.info("sshSession is closed already");
	      }
	    }
	} catch(Exception e) 
	{
		throw new Exception("�Ͽ�SFTP��" + host + "������", e);
	}

  }
 
  /**
   * ���������ļ�
   * @param remotPath��Զ������Ŀ¼(��·�����Ž���,����Ϊ���·��eg:/assess/sftp/jiesuan_2/2014/)
   * @param localPath�����ر���Ŀ¼(��·�����Ž���,D:\Duansha\sftp\)
   * @param fileFormat�������ļ���ʽ(���ض��ַ���ͷ,Ϊ�ղ�������)
   * @param fileEndFormat�������ļ���ʽ(�ļ���ʽ)
   * @param del�����غ��Ƿ�ɾ��sftp�ļ�
   * @return
 * @throws Exception 
   */
  @SuppressWarnings("rawtypes")
public List<String> batchDownLoadFile(String remotePath, String localPath,
      String fileFormat, String fileEndFormat, boolean del) throws Exception
  {
    List<String> filenames = new ArrayList<String>();
    try
    {
      Vector v = listFiles(remotePath);
      if (v.size() > 0)
      {
        Iterator it = v.iterator();
        while (it.hasNext())
        {
          LsEntry entry = (LsEntry) it.next();
          String filename = entry.getFilename();
          SftpATTRS attrs = entry.getAttrs();
          if (!attrs.isDir())
          {
            boolean flag = false;
            String localFileName = localPath + filename;
            fileFormat = fileFormat == null ? "" : fileFormat
                .trim();
            fileEndFormat = fileEndFormat == null ? ""
                : fileEndFormat.trim();
            // �������
            if (fileFormat.length() > 0 && fileEndFormat.length() > 0)
            {
              if (filename.startsWith(fileFormat) && filename.endsWith(fileEndFormat))
              {
                flag = downloadFile(remotePath, filename,localPath, filename);
                if (flag)
                {
                  filenames.add(localFileName);
                  if (flag && del)
                  {
                    deleteSFTP(remotePath, filename);
                  }
                }
              }
            }
            else if (fileFormat.length() > 0 && "".equals(fileEndFormat))
            {
              if (filename.startsWith(fileFormat))
              {
                flag = downloadFile(remotePath, filename, localPath, filename);
                if (flag)
                {
                  filenames.add(localFileName);
                  if (flag && del)
                  {
                    deleteSFTP(remotePath, filename);
                  }
                }
              }
            }
            else if (fileEndFormat.length() > 0 && "".equals(fileFormat))
            {
              if (filename.endsWith(fileEndFormat))
              {
                flag = downloadFile(remotePath, filename,localPath, filename);
                if (flag)
                {
                  filenames.add(localFileName);
                  if (flag && del)
                  {
                    deleteSFTP(remotePath, filename);
                  }
                }
              }
            }
            else
            {
              flag = downloadFile(remotePath, filename,localPath, filename);
              if (flag)
              {
                filenames.add(localFileName);
                if (flag && del)
                {
                  deleteSFTP(remotePath, filename);
                }
              }
            }
          } else if(attrs.isDir() && !filename.startsWith(".")) 
          {
        	  String tempRemotePath = remotePath + filename + "/";
        	  batchDownLoadFile(tempRemotePath, localPath, fileFormat, fileEndFormat, del);
          }
        }
      }
    }
    catch (SftpException e)
    {
    	throw new Exception("���������ļ�����", e);
    }
    return filenames;
  }
 
  /**
   * ���ص����ļ�
   * @param remotPath��Զ������Ŀ¼(��·�����Ž���)
   * @param remoteFileName�������ļ���
   * @param localPath�����ر���Ŀ¼(��·�����Ž���)
   * @param localFileName�������ļ���
   * @return
 * @throws Exception 
   */
  public boolean downloadFile(String remotePath, String remoteFileName,String localPath, String localFileName) throws Exception
  {
    FileOutputStream fieloutput = null;
    try
    {
      File file = new File(localPath + localFileName);
      fieloutput = new FileOutputStream(file);
      sftp.get(remotePath + remoteFileName, fieloutput);
      LOG.info("===DownloadFile:" + remoteFileName + " success from sftp.");
      return true;
    }
    catch (Exception e)
    {
    	throw new Exception("�����ļ�����", e);
    }
    finally
    {
      if (null != fieloutput)
      {
        try
        {
          fieloutput.close();
        }
        catch (IOException e)
        {
          e.printStackTrace();
        }
      }
    }
  }
 
  /**
   * �ϴ������ļ�
   * @param remotePath��Զ�̱���Ŀ¼
   * @param remoteFileName�������ļ���
   * @param localPath�������ϴ�Ŀ¼(��·�����Ž���)
   * @param localFileName���ϴ����ļ���
   * @return
   */
  public boolean uploadFile(String remotePath, String remoteFileName,String localPath, String localFileName)
  {
    FileInputStream in = null;
    try
    {
      createDir(remotePath);
      File file = new File(localPath + localFileName);
      in = new FileInputStream(file);
      sftp.put(in, remoteFileName);
      return true;
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();
    }
    catch (SftpException e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (in != null)
      {
        try
        {
          in.close();
        }
        catch (IOException e)
        {
          e.printStackTrace();
        }
      }
    }
    return false;
  }
 
  /**
   * �����ϴ��ļ�
   * @param remotePath��Զ�̱���Ŀ¼
   * @param localPath�������ϴ�Ŀ¼(��·�����Ž���)
   * @param del���ϴ����Ƿ�ɾ�������ļ�
   * @return
 * @throws Exception 
   */
  public boolean bacthUploadFile(String remotePath, String localPath,
      boolean del) throws Exception
  {
    try
    {
//      connect();
      File file = new File(localPath);
      File[] files = file.listFiles();
      for (int i = 0; i < files.length; i++)
      {
        if (files[i].isFile()
            && files[i].getName().indexOf("bak") == -1)
        {
          if (this.uploadFile(remotePath, files[i].getName(),
              localPath, files[i].getName())
              && del)
          {
            deleteFile(localPath + files[i].getName());
          }
        }
      }
     LOG.info("upload file is success:remotePath=" + remotePath
        + "and localPath=" + localPath + ",file size is "
        + files.length);
      return true;
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      this.disconnect();
    }
    return false;
 
  }
 
  /**
   * ɾ�������ļ�
   * @param filePath
   * @return
   */
  public boolean deleteFile(String filePath)
  {
    File file = new File(filePath);
    if (!file.exists())
    {
      return false;
    }
 
    if (!file.isFile())
    {
      return false;
    }
    boolean rs = file.delete();
    LOG.info("delete file success from local.");
    return rs;
  }
 
  /**
   * ����Ŀ¼
   * @param createpath
   * @return
   */
  public boolean createDir(String createpath)
  {
    try
    {
      if (isDirExist(createpath))
      {
        this.sftp.cd(createpath);
        return true;
      }
      String pathArry[] = createpath.split("/");
      StringBuffer filePath = new StringBuffer("/");
      for (String path : pathArry)
      {
        if (path.equals(""))
        {
          continue;
        }
        filePath.append(path + "/");
        if (isDirExist(filePath.toString()))
        {
          sftp.cd(filePath.toString());
        }
        else
        {
          // ����Ŀ¼
          sftp.mkdir(filePath.toString());
          // ���벢����Ϊ��ǰĿ¼
          sftp.cd(filePath.toString());
        }
 
      }
      this.sftp.cd(createpath);
      return true;
    }
    catch (SftpException e)
    {
      e.printStackTrace();
    }
    return false;
  }
 
  /**
   * �ж�Ŀ¼�Ƿ����
   * @param directory
   * @return
   */
  public boolean isDirExist(String directory)
  {
    boolean isDirExistFlag = false;
    try
    {
      SftpATTRS sftpATTRS = sftp.lstat(directory);
      isDirExistFlag = true;
      return sftpATTRS.isDir();
    }
    catch (Exception e)
    {
      if (e.getMessage().toLowerCase().equals("no such file"))
      {
        isDirExistFlag = false;
      }
    }
    return isDirExistFlag;
  }
 
  /**
   * ɾ��stfp�ļ�
   * @param directory��Ҫɾ���ļ�����Ŀ¼
   * @param deleteFile��Ҫɾ�����ļ�
   * @param sftp
   */
  public void deleteSFTP(String directory, String deleteFile)
  {
    try
    {
      // sftp.cd(directory);
      sftp.rm(directory + deleteFile);
      LOG.info("delete file success from sftp.");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
 
  /**
   * ���Ŀ¼�����ھʹ���Ŀ¼
   * @param path
   */
  public void mkdirs(String path)
  {
    File f = new File(path);
 
    String fs = f.getParent();
 
    f = new File(fs);
 
    if (!f.exists())
    {
      f.mkdirs();
    }
  }
 
  /**
   * �г�Ŀ¼�µ��ļ�
   * 
   * @param directory��Ҫ�г���Ŀ¼
   * @param sftp
   * @return
   * @throws SftpException
   */
  @SuppressWarnings("rawtypes")
public Vector listFiles(String directory) throws SftpException
  {
    return sftp.ls(directory);
  }
 
  public String getHost()
  {
    return host;
  }
 
  public void setHost(String host)
  {
    this.host = host;
  }
 
  public String getUsername()
  {
    return username;
  }
 
  public void setUsername(String username)
  {
    this.username = username;
  }
 
  public String getPassword()
  {
    return password;
  }
 
  public void setPassword(String password)
  {
    this.password = password;
  }
 
  public int getPort()
  {
    return port;
  }
 
  public void setPort(int port)
  {
    this.port = port;
  }
 
  public ChannelSftp getSftp()
  {
    return sftp;
  }
 
  public void setSftp(ChannelSftp sftp)
  {
    this.sftp = sftp;
  }
   
  /**����
 * @throws Exception */
  public static void main(String[] args) throws Exception
  {
    SFTPUtils sftp = null;
    // ���ش�ŵ�ַ
    String localPath = "E:/ftpdownload/";
    // Sftp����·��
    String sftpPath = "/";
    try
    {
      sftp = new SFTPUtils("192.168.0.102", 25, "admin", "admin");
      sftp.connect();
      // ����
      sftp.batchDownLoadFile(sftpPath, localPath, "", ".txt", false);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      sftp.disconnect();
    }
  }
}