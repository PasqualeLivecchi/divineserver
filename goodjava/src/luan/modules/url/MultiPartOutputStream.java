//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//
//  This horrible broken code from jetty is just here for me to look at.  It isn't used.  -fschmidt

package luan.modules.url;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/* ================================================================ */
/** Handle a multipart MIME response.
 *
 * 
 * 
*/
public class MultiPartOutputStream extends FilterOutputStream
{
    /* ------------------------------------------------------------ */
    private static final byte[] __CRLF={'\r','\n'};
    private static final byte[] __DASHDASH={'-','-'};
    
    public static String MULTIPART_MIXED="multipart/mixed";
    public static String MULTIPART_X_MIXED_REPLACE="multipart/x-mixed-replace";
    public static final String __ISO_8859_1="ISO-8859-1";

	public static String newBoundary(Object obj) {
        return "jetty"+System.identityHashCode(obj)+
        Long.toString(System.currentTimeMillis(),36);
	}
    
    /* ------------------------------------------------------------ */
    private final String boundary;
    private final byte[] boundaryBytes;

    /* ------------------------------------------------------------ */
    private boolean inPart=false;    
    
    /* ------------------------------------------------------------ */
    public MultiPartOutputStream(OutputStream out,String boundary)
    throws IOException
    {
        super(out);

		this.boundary = boundary;
        boundaryBytes=boundary.getBytes(__ISO_8859_1);

        inPart=false;
    }

    

    /* ------------------------------------------------------------ */
    /** End the current part.
     * @exception IOException IOException
     */
    @Override
    public void close()
         throws IOException
    {
        if (inPart)
            out.write(__CRLF);
        out.write(__DASHDASH);
        out.write(boundaryBytes);
        out.write(__DASHDASH);
        out.write(__CRLF);
        inPart=false;
        super.close();
    }
    
    /* ------------------------------------------------------------ */
    public String getBoundary()
    {
        return boundary;
    }

    public OutputStream getOut() {return out;}
    
    /* ------------------------------------------------------------ */
    /** Start creation of the next Content.
     */
    public void startPart(String contentType)
         throws IOException
    {
        if (inPart)
            out.write(__CRLF);
        inPart=true;
        out.write(__DASHDASH);
        out.write(boundaryBytes);
        out.write(__CRLF);
        if (contentType != null)
            out.write(("Content-Type: "+contentType).getBytes(__ISO_8859_1));
        out.write(__CRLF);
        out.write(__CRLF);
    }
        
    /* ------------------------------------------------------------ */
    /** Start creation of the next Content.
     */
    public void startPart(String contentType, String[] headers)
         throws IOException
    {
        if (inPart)
            out.write(__CRLF);
        inPart=true;
        out.write(__DASHDASH);
        out.write(boundaryBytes);
        out.write(__CRLF);
        if (contentType != null)
            out.write(("Content-Type: "+contentType).getBytes(__ISO_8859_1));
        out.write(__CRLF);
        for (int i=0;headers!=null && i<headers.length;i++)
        {
            out.write(headers[i].getBytes(__ISO_8859_1));
            out.write(__CRLF);
        }
        out.write(__CRLF);
    }
    
    /* ------------------------------------------------------------ */
    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        out.write(b,off,len);
    }
}




