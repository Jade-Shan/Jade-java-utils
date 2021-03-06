package net.jadedungeon.javautil.net;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.sun.mail.imap.IMAPStore;

import net.jadedungeon.javautil.base.FileOperater;
import net.jadedungeon.javautil.net.email.MailAttachmentHandler;
import net.jadedungeon.javautil.net.email.MailBodyHandler;
import net.jadedungeon.javautil.net.email.MailFolderHandler;
import net.jadedungeon.javautil.net.email.MailUtil;

public class MailUtilTest {

	/*
	 * 网易邮箱开通了IMAP后，再心蓝中使用IMAP收件无法正常， <br/> 提示“NO Select Unsafe Login. Please
	 * contact kefu@188.com for help”。 <br/>
	 * 第三方客户端收信他们就会认为是不安全的，实则是为了推广他们自己的邮件客户端，实属霸王条款。 <br/> <br/>
	 * 网民的力量是强大的，据说从2014.12.20开始，网易提供了偷偷提供了一个入口设置解决这个问题，链接地址： <br/>
	 * http://config.mail.163.com/settings/imap/index.jsp?uid=xxxxxx@163.com <br/>
	 * 
	 * 首先需要先登录网页邮箱，如果是126则自己替换链接中的163。按页面提示，通过手机短信验证之后，出现如下提示: <br/> <br/>
	 * 您可以继续使用当前客户端收发邮件了，请特别注意个人的电子信息安全哦。感谢您对网易邮箱的支持！就可以正常使用第三方邮件客户端的IMAP功能来收件了。
	 * <br/>
	 */

	// @Rule
	// public final GreenMailRule greenMail = new GreenMailRule(GREEN_MAIL_SETUP);
	private GreenMail greenMail = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		initMailCfg();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		greenMail = new GreenMail(GREEN_MAIL_SETUP);
		greenMail.start();

		// Create user, as connect verifies pwd
		greenMail.setUser(email_greenmail_FROM, email_greenmail_FROM, password_greenmail_FROM);
		greenMail.setUser(email_greenmail_TO, email_greenmail_TO, password_greenmail_TO);

		Session smtpSession = greenMail.getSmtp().createSession();

		Message msg = null;
		// simple text message
		msg = new MimeMessage(smtpSession);
		msg.setFrom(new InternetAddress(email_greenmail_FROM));
		msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email_greenmail_TO));
		msg.setSubject("Test Pain-Text email");
		msg.setText("This is Text mail");
		Transport.send(msg);

		// muti part message
		msg = new MimeMessage(smtpSession);
		msg.setFrom(new InternetAddress(email_greenmail_FROM));
		msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email_greenmail_TO));
		msg.setSubject("Test Muti-Part email");
		MimeMultipart multipart = new MimeMultipart();
		final MimeBodyPart part1 = new MimeBodyPart();
		part1.setContent("Pain-text preview in Muti-Part email", "text/plain;charset=UTF-8");
		multipart.addBodyPart(part1);
		final MimeBodyPart part2 = new MimeBodyPart();
		part2.setContent("<br>HTML Source in  Muti-Part email</br>", "text/html;charset=UTF-8");
		multipart.addBodyPart(part2);
		msg.setContent(multipart);
		Transport.send(msg);
	}

	@After
	public void tearDown() throws Exception {
		greenMail.stop();
	}

	@Test
	public void testGetGreenMail() throws Exception {
		String mailTo = email_greenmail_TO;
		String pwdTo = password_greenmail_TO;
		Properties mailRcvProp = SERVER_CFGS.get("greenmail_imap");

		// String mailTo = "xxx@163.com";
		// String pwdTo = "xxx";
		// Properties mailRcvProp = SERVER_CFGS.get("163_imaps");

		MailUtil.receiveMail(mailRcvProp, mailTo, pwdTo, true, defaultFolderHandler, defaultBodyHandler,
				defaultAttachmentHandler);

		// Alternative 1: Create session and store or ...
		Session imapSession = greenMail.getImap().createSession();
		Store store = imapSession.getStore("imap");
		store.connect(mailTo, pwdTo);
		Folder inbox = store.getFolder("INBOX");
		inbox.open(Folder.READ_ONLY);
		Message msgReceived = inbox.getMessage(1);
		assertEquals("Test Pain-Text email", msgReceived.getSubject());
		store.close();

		// Alternative 2: ... let GreenMail create and configure a store:
		IMAPStore imapStore = greenMail.getImap().createStore();
		imapStore.connect(mailTo, pwdTo);
		inbox = imapStore.getFolder("INBOX");
		inbox.open(Folder.READ_ONLY);
		msgReceived = inbox.getMessage(1);
		assertEquals("Test Pain-Text email", msgReceived.getSubject());
		imapStore.close();

		// Alternative 3: ... directly fetch sent message using GreenMail API
		assertEquals(2, greenMail.getReceivedMessagesForDomain(mailTo).length);
		msgReceived = greenMail.getReceivedMessagesForDomain(mailTo)[0];
		assertEquals("Test Pain-Text email", msgReceived.getSubject());

	}

	@Test
	public void testSendGreenMail() throws Exception {

		String mailFrom = email_greenmail_FROM;
		String pwdFrom = password_greenmail_FROM;
		String mailTo = email_greenmail_TO;
		String pwdTo = password_greenmail_TO;
		Properties mailSendProp = SERVER_CFGS.get("greenmail_smtp");
		Properties mailRcvProp = SERVER_CFGS.get("greenmail_imap");

		// String mailFrom = "xxx@163.com";
		// String pwdFrom = "xxx";
		// Properties mailSendProp = SERVER_CFGS.get("163_smtps");
		// String mailTo = "xxx@163.com";
		// String pwdTo = "xxx";
		// Properties mailRcvProp = SERVER_CFGS.get("163_imaps");

		Map<String, DataSource> attachments = new HashMap<>();
		attachments.put("Documents.pdf", new FileDataSource(new File("src/test/resources/sample1.pdf")));

		Map<String, DataSource> related = new HashMap<>();
		related.put("img2.jpg", new FileDataSource(new File("src/test/resources/sample2.jpg")));
		related.put("img3.jpg", new FileDataSource(new File("src/test/resources/sample3.jpg")));

		String[] toArr = { mailTo };
		String[] ccArr = {};
		MailUtil.sendTextMail(mailSendProp, mailFrom, pwdFrom, toArr, ccArr, ccArr, //
				"包裹3", "李总,您好;你的包裹在前台");
		MailUtil.sendTextMail(mailSendProp, mailFrom, pwdFrom, toArr, ccArr, ccArr, //
				"包裹4", "李总,您好;你的包裹在前台", attachments);
		MailUtil.sendHtmlMail(mailSendProp, mailFrom, pwdFrom, toArr, ccArr, ccArr, //
				"包裹5", "<h1>李总,您好;你的包裹在前台</h1>", null, null);
		MailUtil.sendHtmlMail(mailSendProp, mailFrom, pwdFrom, toArr, ccArr, ccArr, //
				"包裹6", "<h1>李总,您好;你的包裹在前台</h1>" + //
						"<br><img src='cid:img2.jpg'/><br>" //
						+ "<img src='cid:img3.jpg'/>",
				related, attachments);

		IMAPStore imapStore = greenMail.getImap().createStore();
		imapStore.connect(mailTo, pwdTo);
		Folder inbox = imapStore.getFolder("INBOX");
		inbox.open(Folder.READ_ONLY);
		//
		Message msgReceived = inbox.getMessage(3);
		assertEquals("包裹3", msgReceived.getSubject());
		//
		msgReceived = inbox.getMessage(4);
		assertEquals("包裹4", msgReceived.getSubject());
		//
		msgReceived = inbox.getMessage(5);
		assertEquals("包裹5", msgReceived.getSubject());
		//
		msgReceived = inbox.getMessage(6);
		assertEquals("包裹6", msgReceived.getSubject());

		//
		imapStore.close();

		MailUtil.receiveMail(mailRcvProp, mailTo, pwdTo, true, defaultFolderHandler, defaultBodyHandler,
				defaultAttachmentHandler);
	}

	@Test
	public void testMoveMail() throws Exception {
		// // String fromStr = "lab-booking-unread";
		// // String toStr = "lab-booking-success";
		// String fromStr = "INBOX";
		// String toStr = "草稿箱";
		// // 创建Session实例对象
		// Session session = Session.getInstance(SERVER_CFGS.get("163_imap"));
		// // 创建IMAP协议的Store对象
		// Store store = session.getStore("imap");
		// // 连接邮件服务器
		// store.connect(email_163, password_163);
		// // 获得收件箱
		// Folder from = store.getFolder(fromStr);
		// from.open(Folder.READ_WRITE);
		// Folder to = store.getFolder(toStr);
		// to.open(Folder.READ_WRITE);
		//
		// Message[] mails = from.getMessages();
		// if (null != mails && mails.length > 0) {
		// String subject = MailUtil.getSubject(mails[0]);
		// if (subject.indexOf("Move to") > -1) {
		// Message[] mr = { mails[0] };
		// from.copyMessages(mr, to);
		// mails[0].setFlag(Flags.Flag.DELETED, true);// 设置已删除状态为true
		// }
		// }
		// from.close(true); // ture表示对标记了删除记录的邮件实施删除操作
		// to.close(true);
		// store.close();
	}

	private MailFolderHandler defaultFolderHandler = new MailFolderHandler() {

		@Override
		public void handleMailFolder(Folder folder, MailBodyHandler bodyHandler, MailAttachmentHandler athmHandler)
				throws IOException //
		{
			try {
				// 获得收件箱的邮件列表
				Message[] messages = folder.getMessages();
				// 打印不同状态的邮件数量
				System.out.println("<" + folder.getFullName() + "> 中有" + messages.length + "封邮件!");
				System.out.println("其中有" + folder.getUnreadMessageCount() + "封未读邮件!");
				System.out.println("其中有" + folder.getNewMessageCount() + "封新邮件!");
				System.out.println("其中有" + folder.getDeletedMessageCount() + "封已删除邮件!");
				if (null != messages && messages.length > 0) {
					for (Message msg : messages) {
						MailUtil.parseMessage(bodyHandler, athmHandler, msg); // 解析邮件
					}
				}
			} catch (Exception e) {
				throw new IOException(e.getMessage());
			}
		}
	};

	private MailBodyHandler defaultBodyHandler = new MailBodyHandler() {
		@Override
		public void handleMailBody(Message msg) throws IOException {
			try {
				System.out.println("------------------解析第" + msg.getMessageNumber() + "封邮件-------------------- ");
				System.out.println("主题: " + MailUtil.getSubject(msg));
				System.out.println("发件人: " + MailUtil.getFrom(msg));
				System.out.println("收件人：" + MailUtil.getReceiveAddress(msg, null));
				System.out.println("发送时间：" + MailUtil.getSentDate(msg, null));
				System.out.println("是否已读：" + MailUtil.isSeen(msg));
				System.out.println("邮件优先级：" + MailUtil.getPriority(msg));
				System.out.println("是否需要回执：" + MailUtil.isReplySign(msg));
				System.out.println("是否包含附件：" + MailUtil.hasAttachment(msg));
				System.out.println("邮件大小：" + msg.getSize() * 1024 + "kb");
				/*
				 * 处理文本
				 */
				Map<String, StringBuffer> content = new HashMap<>();
				MailUtil.parseMailTextContent(msg, content);
				if (content.containsKey(MailUtil.MAIL_TYPE_TEXT_PAIN)) {
					String s1 = content.get(MailUtil.MAIL_TYPE_TEXT_PAIN).toString();
					s1 = s1.length() < 200 ? s1 : s1.substring(0, 200);
					System.out.println("邮件正文预览：" + s1);
				}
				if (content.containsKey(MailUtil.MAIL_TYPE_TEXT_HTML)) {
					String s1 = content.get(MailUtil.MAIL_TYPE_TEXT_HTML).toString();
					s1 = s1.length() < 200 ? s1 : s1.substring(0, 200);
					System.out.println("邮件正文HTML：" + s1);
				}
				/*
				 * 设置已读标志 <br/> msg.setFlag(Flag.SEEN, true);
				 */
				/*
				 * 删除邮件 <br/> msg.setFlag(Flag.DELETED, true);
				 */
				System.out.println("------------------第" + msg.getMessageNumber() + "封邮件解析结束，并将其标记为已读 ---------- \n");
			} catch (Exception e) {
				e.printStackTrace();
				throw new IOException(e.getMessage());
			}
		}
	};

	private MailAttachmentHandler defaultAttachmentHandler = new MailAttachmentHandler() {
		@Override
		public void handleMailAttachment(Message msg, BodyPart bodyPart) throws IOException {
			try {
				InputStream is = bodyPart.getInputStream();
				String subject = URLEncoder.encode(MailUtil.getSubject(msg), "UTF-8");
				String filename = URLEncoder.encode(MailUtil.decodeText(//
						bodyPart.getFileName()), "UTF-8");
				FileOperater.writeFile("/mailtmp/", System.currentTimeMillis() + "." + subject + "_" + filename, is);
			} catch (Exception e) {
				throw new IOException(e.getMessage());
			}
		}
	};

	private static final String email_greenmail_FROM = "bar@example.com";
	private static final String password_greenmail_FROM = "secret-pwd";
	private static final String email_greenmail_TO = "foo@example.com";
	private static final String password_greenmail_TO = "secret-pwd";

	private static final Map<String, Properties> SERVER_CFGS = new HashMap<>();

	private ServerSetup[] GREEN_MAIL_SETUP = { //
			new ServerSetup(3025, "localhost", "smtp"), //
			new ServerSetup(3110, "localhost", "pop3"), //
			new ServerSetup(3143, "localhost", "imap"), //
			new ServerSetup(3465, "localhost", "smtps"), //
			new ServerSetup(3995, "localhost", "pop3s"), //
			new ServerSetup(3993, "localhost", "imaps") //
	};

	private static void initMailCfg() {
		Properties p = null;
		// 准备连接服务器的会话信息
		p = new Properties();
		p.setProperty("mail.store.protocol", "imap");
		p.setProperty("mail.imap.ssl.enable", "false");
		p.setProperty("mail.imap.host", "imap.163.com");
		p.setProperty("mail.imap.port", "143");
		SERVER_CFGS.put("163_imap", p);

		// 准备连接服务器的会话信息
		p = new Properties();
		p.setProperty("mail.store.protocol", "pop3");
		p.setProperty("mail.pop3.ssl.enable", "false");
		p.setProperty("mail.pop3.host", "pop3.163.com");
		p.setProperty("mail.pop3.port", "110");
		SERVER_CFGS.put("163_pop3", p);

		// 准备连接服务器的会话信息
		p = new Properties();
		p.setProperty("mail.store.protocol", "imap");
		p.setProperty("mail.imap.ssl.enable", "true");
		p.setProperty("mail.imap.host", "imap.163.com");
		p.setProperty("mail.imap.port", "993");
		p.setProperty("mail.imap.auth.plain.disable", "true");
		SERVER_CFGS.put("163_imaps", p);

		// 准备连接服务器的会话信息
		p = new Properties();
		p.setProperty("mail.store.protocol", "pop3");
		p.setProperty("mail.pop3.ssl.enable", "true");
		p.setProperty("mail.pop3.host", "pop3.163.com");
		p.setProperty("mail.pop3.port", "995");
		p.setProperty("mail.pop3.auth.plain.disable", "true");
		SERVER_CFGS.put("163_pop3s", p);

		// 准备连接服务器的会话信息
		p = new Properties();
		p.setProperty("mail.smtp.host", "smtp.163.com");
		p.setProperty("mail.smtp.port", "25");
		// p.setProperty("mail.smtp.socketFactory.port", "25");
		p.setProperty("mail.smtp.auth", "true");
		// p.setProperty("mail.smtp.socketFactory.class", "SSL_FACTORY");
		SERVER_CFGS.put("163_smtp", p);

		// 准备连接服务器的会话信息
		p = new Properties();
		p.setProperty("mail.smtp.host", "smtp.163.com");
		p.setProperty("mail.smtp.port", "465");
		p.setProperty("mail.smtp.socketFactory.port", "465");
		p.setProperty("mail.smtp.auth", "true");
		p.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		p.setProperty("mail.smtp.socketFactory.fallback", "false");
		SERVER_CFGS.put("163_smtps", p);

		// 准备连接服务器的会话信息
		p = new Properties();
		p.setProperty("mail.store.protocol", "imap");
		p.setProperty("mail.imap.ssl.enable", "false");
		p.setProperty("mail.imap.host", "127.0.0.1");
		p.setProperty("mail.imap.port", "3143");
		SERVER_CFGS.put("greenmail_imap", p);

		// 准备连接服务器的会话信息
		p = new Properties();
		p.setProperty("mail.store.protocol", "pop3");
		p.setProperty("mail.pop3.ssl.enable", "false");
		p.setProperty("mail.pop3.host", "127.0.0.1");
		p.setProperty("mail.pop3.port", "3110");
		SERVER_CFGS.put("greenmail_pop3", p);

		// 准备连接服务器的会话信息
		p = new Properties();
		p.setProperty("mail.smtp.host", "127.0.0.1");
		p.setProperty("mail.smtp.port", "3025");
		p.setProperty("mail.smtp.auth", "false");
		SERVER_CFGS.put("greenmail_smtp", p);
	}
}
