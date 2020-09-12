/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package goodjava.io;

import java.io.InputStream;
import java.io.IOException;

/**
 * A <code>BufferedInputStream</code> adds
 * functionality to another input stream-namely,
 * the ability to buffer the input. When  the <code>BufferedInputStream</code>
 * is created, an internal buffer array is
 * created. As bytes  from the stream are read
 * or skipped, the internal buffer is refilled
 * as necessary  from the contained input stream,
 * many bytes at a time.
 *
 * @author  Arthur van Hoff
 * @since   JDK1.0
 */
public final class BufferedInputStream extends NoMarkInputStream {
	private final byte buf[];

	/**
	 * The index one greater than the index of the last valid byte in
	 * the buffer.
	 * This value is always
	 * in the range <code>0</code> through <code>buf.length</code>;
	 * elements <code>buf[0]</code>  through <code>buf[count-1]
	 * </code>contain buffered input data obtained
	 * from the underlying input stream.
	 */
	private int count;

	/**
	 * The current position in the buffer. This is the index of the next
	 * character to be read from the <code>buf</code> array.
	 * <p>
	 * This value is always in the range <code>0</code>
	 * through <code>count</code>. If it is less
	 * than <code>count</code>, then  <code>buf[pos]</code>
	 * is the next byte to be supplied as input;
	 * if it is equal to <code>count</code>, then
	 * the  next <code>read</code> or <code>skip</code>
	 * operation will require more bytes to be
	 * read from the contained  input stream.
	 *
	 * @see     java.io.BufferedInputStream#buf
	 */
	private int pos;

	/**
	 * Creates a <code>BufferedInputStream</code>
	 * and saves its  argument, the input stream
	 * <code>in</code>, for later use. An internal
	 * buffer array is created and  stored in <code>buf</code>.
	 *
	 * @param   in   the underlying input stream.
	 */
	public BufferedInputStream(InputStream in) {
		this(in, 8192);
	}

	/**
	 * Creates a <code>BufferedInputStream</code>
	 * with the specified buffer size,
	 * and saves its  argument, the input stream
	 * <code>in</code>, for later use.  An internal
	 * buffer array of length  <code>size</code>
	 * is created and stored in <code>buf</code>.
	 *
	 * @param   in     the underlying input stream.
	 * @param   size   the buffer size.
	 */
	public BufferedInputStream(InputStream in, int size) {
		super(in);
		buf = new byte[size];
	}

	/**
	 * Fills the buffer with more data.
	 * This method also assumes that all data has already been read in,
	 * hence pos > count.
	 */
	private void fill() throws IOException {
		pos = 0;
		count = 0;
		int n = in.read(buf, 0, buf.length);
		if (n > 0)
			count = n;
	}

	/**
	 * See
	 * the general contract of the <code>read</code>
	 * method of <code>InputStream</code>.
	 *
	 * @return     the next byte of data, or <code>-1</code> if the end of the
	 *             stream is reached.
	 * @exception  IOException  if this input stream has been closed by
	 *                          invoking its {@link #close()} method,
	 *                          or an I/O error occurs.
	 * @see        java.io.FilterInputStream#in
	 */
	public int read() throws IOException {
		if (pos >= count) {
			fill();
			if (pos >= count)
				return -1;
		}
		return buf[pos++] & 0xff;
	}

	/**
	 * Read characters into a portion of an array, reading from the underlying
	 * stream at most once if necessary.
	 */
	private int read1(byte[] b, int off, int len) throws IOException {
		int cnt = 0;
		int avail = count - pos;
		if( avail > 0 ) {
			cnt = (avail < len) ? avail : len;
			System.arraycopy(buf, pos, b, off, cnt);
			pos += cnt;
			len -= cnt;
			if( len == 0 )
				return cnt;
			off += cnt;
		}
		if (len >= buf.length) {
			return cnt + Math.max( 0, in.read(b, off, len) );
		}
		fill();
		if (count <= 0)
			return cnt;
		System.arraycopy(buf, 0, b, off, len);
		pos += len;
		return cnt + len;
	}

	public int read(byte[] b, int off, int len) throws IOException {
		if( len == 0 )
			return 0;
		int n = read1(b,off,len);
		return n==0 ? -1 : n;
	}

	/**
	 * See the general contract of the <code>skip</code>
	 * method of <code>InputStream</code>.
	 *
	 * @exception  IOException  if the stream does not support seek,
	 *                          or if this input stream has been closed by
	 *                          invoking its {@link #close()} method, or an
	 *                          I/O error occurs.
	 */
	public long skip(long n) throws IOException {
		if( n <= 0 )
			return 0;
		long skipped = 0;
		long avail = count - pos;
		if( avail > 0 ) {
			skipped = (avail < n) ? avail : n;
			pos += skipped;
			n -= skipped;
			if( n == 0 )
				return skipped;
		}
		return skipped + in.skip(n);
	}

	/**
	 * Returns an estimate of the number of bytes that can be read (or
	 * skipped over) from this input stream without blocking by the next
	 * invocation of a method for this input stream. The next invocation might be
	 * the same thread or another thread.  A single read or skip of this
	 * many bytes will not block, but may read or skip fewer bytes.
	 * <p>
	 * This method returns the sum of the number of bytes remaining to be read in
	 * the buffer (<code>count&nbsp;- pos</code>) and the result of calling the
	 * {@link java.io.FilterInputStream#in in}.available().
	 *
	 * @return     an estimate of the number of bytes that can be read (or skipped
	 *             over) from this input stream without blocking.
	 * @exception  IOException  if this input stream has been closed by
	 *                          invoking its {@link #close()} method,
	 *                          or an I/O error occurs.
	 */
	public int available() throws IOException {
		int n = count - pos;
		int avail = in.available();
		return n > (Integer.MAX_VALUE - avail)
					? Integer.MAX_VALUE
					: n + avail;
	}

}
