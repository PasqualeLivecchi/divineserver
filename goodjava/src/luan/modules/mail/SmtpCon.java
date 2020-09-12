package luan.modules.mail;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeBodyPart;
import luan.Luan;
import luan.LuanTable;
import luan.LuanException;


public final class SmtpCon {
	private final Session session;

	public SmtpCon(LuanTable paramsTbl) throws LuanException {
		Map<Object,Object> params = paramsTbl.asMap();
		Properties props = new Properties(System.getProperties());

		String host = getString(params,"host");
		if( host==null )
			throw new LuanException( "parameter 'host' is required" );
		props.setProperty("mail.smtp.host",host);

		Object port = params.remove("port");
		if( port != null ) {
			String s;
			if( port instanceof String ) {
				s = (String)port;
			} else if( port instanceof Number ) {
				Integer i = Luan.asInteger(port);
				if( i == null )
					throw new LuanException( "parameter 'port' must be an integer" );
				s = i.toString();
			} else {
				throw new LuanException( "parameter 'port' must be an integer" );
			}
			props.setProperty("mail.smtp.socketFactory.port", s);
			props.setProperty("mail.smtp.port", s);
		}

		String username = getString(params,"username");
		if( username == null ) {
			session = Session.getInstance(props);
		} else {
			String password = getString(params,"password");
			if( password==null )
				throw new LuanException( "parameter 'password' is required with 'username'" );
			props.setProperty("mail.smtp.auth","true");
			final PasswordAuthentication pa = new PasswordAuthentication(username,password);
			Authenticator auth = new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return pa;
				}
			};
			session = Session.getInstance(props,auth);
		}

		if( !params.isEmpty() )
			throw new LuanException( "unrecognized parameters: "+params );
	}

	private static String getString(Map<Object,Object> params,String key) throws LuanException {
		Object val = params.remove(key);
		if( val!=null && !(val instanceof String) )
			throw new LuanException( "parameter '"+key+"' must be a string" );
		return (String)val;
	}


	public void send(LuanTable mailTbl) throws LuanException {
		try {
			Map<Object,Object> mailParams = mailTbl.asMap();
			MimeMessage msg = new MimeMessage(session);

			String from = getString(mailParams,"from");
			if( from != null )
				msg.setFrom(from);

			String to = getString(mailParams,"to");
			if( to != null )
				msg.setRecipients(Message.RecipientType.TO,to);

			String replyTo = getString(mailParams,"reply-to");
			if( replyTo != null )
				msg.setReplyTo(InternetAddress.parse(replyTo));

			String cc = getString(mailParams,"cc");
			if( cc != null )
				msg.setRecipients(Message.RecipientType.CC,cc);

			String subject = getString(mailParams,"subject");
			if( subject != null )
				msg.setSubject(subject);

			Object body = mailParams.remove("body");
			Object attachments = mailParams.remove("attachments");
			Part bodyPart = attachments==null ? msg : new MimeBodyPart();

			if( body != null ) {
				if( body instanceof String ) {
					bodyPart.setText((String)body);
				} else if( body instanceof LuanTable ) {
					LuanTable bodyTbl = (LuanTable)body;
					Map<Object,Object> map = bodyTbl.asMap();
					MimeMultipart mp = new MimeMultipart("alternative");
					String text = (String)map.remove("text");
					if( text != null ) {
						MimeBodyPart part = new MimeBodyPart();
						part.setText(text);
						mp.addBodyPart(part);
					}
					String html = (String)map.remove("html");
					if( html != null ) {
						MimeBodyPart part = new MimeBodyPart();
						part.setContent(html,"text/html");
						mp.addBodyPart(part);
					}
					if( !map.isEmpty() )
						throw new LuanException( "invalid body types: " + map );
					bodyPart.setContent(mp);
				} else
					throw new LuanException( "parameter 'body' must be a string or table" );
			}

			if( attachments != null ) {
				if( !(attachments instanceof LuanTable) )
					throw new LuanException( "parameter 'attachments' must be a table" );
				LuanTable attachmentsTbl = (LuanTable)attachments;
				if( !attachmentsTbl.isList() )
					throw new LuanException( "parameter 'attachments' must be a list" );
				MimeMultipart mp = new MimeMultipart("mixed");
				if( body != null )
					mp.addBodyPart((MimeBodyPart)bodyPart);
				for( Object attachment : attachmentsTbl.asList() ) {
					if( !(attachment instanceof LuanTable) )
						throw new LuanException( "each attachment must be a table" );
					Map<Object,Object> attachmentMap = ((LuanTable)attachment).asMap();
					Object obj;

					obj = attachmentMap.remove("filename");
					if( obj==null )
						throw new LuanException( "an attachment is missing 'filename'" );
					if( !(obj instanceof String) )
						throw new LuanException( "an attachment filename must be a string" );
					String filename = (String)obj;

					obj = attachmentMap.remove("content_type");
					if( obj==null )
						throw new LuanException( "an attachment is missing 'content_type'" );
					if( !(obj instanceof String) )
						throw new LuanException( "an attachment content_type must be a string" );
					String content_type = (String)obj;

					Object content = attachmentMap.remove("content");
					if( content==null )
						throw new LuanException( "an attachment is missing 'content'" );
					if( content_type.startsWith("text/") && content instanceof byte[] )
						content = new String((byte[])content);

					if( !attachmentMap.isEmpty() )
						throw new LuanException( "unrecognized attachment parameters: "+attachmentMap );

					MimeBodyPart part = new MimeBodyPart();
					part.setContent(content,content_type);
					part.setFileName(filename);
					mp.addBodyPart(part);
				}
				msg.setContent(mp);
			}

			if( !mailParams.isEmpty() )
				throw new LuanException( "unrecognized parameters: "+mailParams );

			Transport.send(msg);
		} catch(MessagingException e) {
			throw new LuanException(e);
		}
	}

}
