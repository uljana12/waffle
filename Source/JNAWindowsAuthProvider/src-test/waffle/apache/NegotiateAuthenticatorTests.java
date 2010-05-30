/*
 * Copyright (c) Application Security Inc., 2010
 * All Rights Reserved
 * Eclipse Public License (EPLv1)
 * http://waffle.codeplex.com/license
 */
package waffle.apache;

import org.apache.catalina.Realm;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Sspi;
import com.sun.jna.platform.win32.Sspi.SecBufferDesc;

import waffle.apache.NegotiateAuthenticator;
import waffle.apache.catalina.SimpleContext;
import waffle.apache.catalina.SimpleHttpRequest;
import waffle.apache.catalina.SimpleHttpResponse;
import waffle.apache.catalina.SimpleRealm;
import waffle.util.Base64;
import waffle.windows.auth.IWindowsCredentialsHandle;
import waffle.windows.auth.impl.WindowsCredentialsHandleImpl;
import waffle.windows.auth.impl.WindowsSecurityContextImpl;

import junit.framework.TestCase;

/**
 * Waffle Tomcat Authenticator Tests
 * @author dblock[at]dblock[dot]org
 */
public class NegotiateAuthenticatorTests extends TestCase {

	NegotiateAuthenticator _authenticator = null;
	
	@Override
	public void setUp() {
		_authenticator = new NegotiateAuthenticator();
		SimpleContext ctx = new SimpleContext();
		Realm realm = new SimpleRealm();
		ctx.setRealm(realm);
		_authenticator.setContainer(ctx);
		_authenticator.start();
	}

	@Override
	public void tearDown() {
		_authenticator.stop();
		_authenticator = null;
	}
	
	public void testGetInfo() {
		assertTrue(_authenticator.getInfo().length() > 0);		
	}

	public void testChallengeGET() {
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setMethod("GET");
		SimpleHttpResponse response = new SimpleHttpResponse();
		_authenticator.authenticate(request, response, null);
		String[] wwwAuthenticates = response.getHeaderValues("WWW-Authenticate");
		assertEquals(2, wwwAuthenticates.length);
		assertEquals("Negotiate", wwwAuthenticates[0]);
		assertEquals("NTLM", wwwAuthenticates[1]);
		assertEquals("close", response.getHeader("Connection"));
		assertEquals(2, response.getHeaderNames().length);
		assertEquals(401, response.getStatus());
	}
	
	public void testChallengePOST() {
		String securityPackage = "Negotiate";
		IWindowsCredentialsHandle clientCredentials = null;
		WindowsSecurityContextImpl clientContext = null;
		try {			
			clientCredentials = WindowsCredentialsHandleImpl.getCurrent(securityPackage);
			clientCredentials.initialize();
			// initial client security context
			clientContext = new WindowsSecurityContextImpl();
			clientContext.setPrincipalName(Advapi32Util.getUserName());
			clientContext.setCredentialsHandle(clientCredentials.getHandle());
			clientContext.setSecurityPackage(securityPackage);
			clientContext.initialize();
			SimpleHttpRequest request = new SimpleHttpRequest();
			request.setMethod("POST");
			request.setContentLength(0);
			String clientToken = Base64.encode(clientContext.getToken());
			request.addHeader("Authorization", securityPackage + " " + clientToken);
			SimpleHttpResponse response = new SimpleHttpResponse();
			_authenticator.authenticate(request, response, null);
			assertTrue(response.getHeader("WWW-Authenticate").startsWith(securityPackage + " "));
			assertEquals("keep-alive", response.getHeader("Connection"));
			assertEquals(2, response.getHeaderNames().length);
			assertEquals(401, response.getStatus());
		} finally {
			if (clientContext != null) {
				clientContext.dispose();
			}
			if (clientCredentials != null) {
				clientCredentials.dispose();
			}			
		}
	}

	public void testPOSTEmpty() {
		String securityPackage = "Negotiate";
		IWindowsCredentialsHandle clientCredentials = null;
		WindowsSecurityContextImpl clientContext = null;
		try {
			// client credentials handle
			clientCredentials = WindowsCredentialsHandleImpl.getCurrent(securityPackage);
			clientCredentials.initialize();
			// initial client security context
			clientContext = new WindowsSecurityContextImpl();
			clientContext.setPrincipalName(Advapi32Util.getUserName());
			clientContext.setCredentialsHandle(clientCredentials.getHandle());
			clientContext.setSecurityPackage(securityPackage);
			clientContext.initialize();
			// negotiate
			boolean authenticated = false;
			SimpleHttpRequest request = new SimpleHttpRequest();
			request.setMethod("POST");
			request.setContentLength(0);
	        while(true)
	        {
	    		String clientToken = Base64.encode(clientContext.getToken());
	    		request.addHeader("Authorization", securityPackage + " " + clientToken);
	    		
	    		SimpleHttpResponse response = new SimpleHttpResponse();
	    		authenticated = _authenticator.authenticate(request, response, null);
	
	    		if (authenticated) {
	        		assertEquals(0, response.getHeaderNames().length);
	    			break;
	    		}
	    		
	    		assertTrue(response.getHeader("WWW-Authenticate").startsWith(securityPackage + " "));
	    		assertEquals("keep-alive", response.getHeader("Connection"));
	    		assertEquals(2, response.getHeaderNames().length);
	    		assertEquals(401, response.getStatus());
	    		String continueToken = response.getHeader("WWW-Authenticate").substring(securityPackage.length() + 1);
	    		byte[] continueTokenBytes = Base64.decode(continueToken);
	    		assertTrue(continueTokenBytes.length > 0);
	            SecBufferDesc continueTokenBuffer = new SecBufferDesc(Sspi.SECBUFFER_TOKEN, continueTokenBytes);
	            clientContext.initialize(clientContext.getHandle(), continueTokenBuffer);
	        }
	        assertTrue(authenticated);
		} finally {
			if (clientContext != null) {
				clientContext.dispose();
			}
			if (clientCredentials != null) {
				clientCredentials.dispose();
			}			
		}
	}
	
	public void testNegotiate() {
		String securityPackage = "Negotiate";
		IWindowsCredentialsHandle clientCredentials = null;
		WindowsSecurityContextImpl clientContext = null;
		try {
			// client credentials handle
			clientCredentials = WindowsCredentialsHandleImpl.getCurrent(securityPackage);
			clientCredentials.initialize();
			// initial client security context
			clientContext = new WindowsSecurityContextImpl();
			clientContext.setPrincipalName(Advapi32Util.getUserName());
			clientContext.setCredentialsHandle(clientCredentials.getHandle());
			clientContext.setSecurityPackage(securityPackage);
			clientContext.initialize();
			// negotiate
			boolean authenticated = false;
			SimpleHttpRequest request = new SimpleHttpRequest();
	        while(true)
	        {
	    		String clientToken = Base64.encode(clientContext.getToken());
	    		request.addHeader("Authorization", securityPackage + " " + clientToken);
	    		
	    		SimpleHttpResponse response = new SimpleHttpResponse();
	    		authenticated = _authenticator.authenticate(request, response, null);
	
	    		if (authenticated) {
	        		assertEquals(0, response.getHeaderNames().length);
	    			break;
	    		}
	    		
	    		assertTrue(response.getHeader("WWW-Authenticate").startsWith(securityPackage + " "));
	    		assertEquals("keep-alive", response.getHeader("Connection"));
	    		assertEquals(2, response.getHeaderNames().length);
	    		assertEquals(401, response.getStatus());
	    		String continueToken = response.getHeader("WWW-Authenticate").substring(securityPackage.length() + 1);
	    		byte[] continueTokenBytes = Base64.decode(continueToken);
	    		assertTrue(continueTokenBytes.length > 0);
	            SecBufferDesc continueTokenBuffer = new SecBufferDesc(Sspi.SECBUFFER_TOKEN, continueTokenBytes);
	            clientContext.initialize(clientContext.getHandle(), continueTokenBuffer);
	        }
	        assertTrue(authenticated);
		} finally {
			if (clientContext != null) {
				clientContext.dispose();
			}
			if (clientCredentials != null) {
				clientCredentials.dispose();
			}			
		}
	}
}