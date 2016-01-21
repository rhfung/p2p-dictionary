package com.rhfung.logging;

//P2PDictionary
//Copyright (C) 2016, Richard H Fung (www.richardhfung.com)
//
//Permission is hereby granted to any person obtaining a copy of this software 
//and associated documentation files (the "Software"), to deal in the Software 
//for the sole purposes of PERSONAL USE. This software cannot be used in 
//products where commercial interests exist (i.e., license, profit from, or
//otherwise seek monetary value). The person DOES NOT HAVE the right to
//redistribute, copy, modify, merge, publish, sublicense, or sell this Software
//without explicit prior permission from the author, Richard H Fung.
//
//The above copyright notice and this permission notice shall be included 
//in all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//THE SOFTWARE.

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.rhfung.Interop.MemoryStream;


public class LogInstructions
{
    private static SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private PrintStream m_writer;
    private int m_min_level = 0;
    private boolean m_autoFlush = false;
    private int m_id = 0;

    public static final int DEBUG = 0;  // all payload data
    public static final int INFO = 1;   // connection messages
    public static final int WARN = 2;   // warnings only
    public static final int ERROR = 3;  // errors only

    public LogInstructions(OutputStream writer, int min_level_to_log, int dictID, boolean autoFlush)
    {
        m_writer = new PrintStream(writer);
        m_min_level = min_level_to_log;
        m_autoFlush = autoFlush;
        m_id = dictID;
    }

    public PrintStream GetTextWriter()
    {
        return m_writer;
    }

    private static String LevelStringFromInteger(int level) {
        if (level == DEBUG) {
            return "DEBUG";
        } else if (level == INFO) {
            return "INFO";
        } else if (level == WARN) {
            return "WARN";
        } else if (level == ERROR) {
            return "ERROR";
        } else {
            return String.valueOf(level);
        }
    }

    private String getPreamble(int level) {
        return DateTimeNowTicks() + " [" + LevelStringFromInteger(level) + "] (" + m_id + ") {" + Thread.currentThread().getId()  + "} ";
    }

    /**
     * Writes a log message, if the level of the message matches/exceeds initial configuration.
     * Auto-flush is controlled on creation.
     * @param level
     * @param message
     * @param flushThisMessage
     */
    public synchronized void Log(int level, String message, boolean flushThisMessage)
    {
        if (level >= m_min_level)
        {
        	synchronized (m_writer)
            {
                m_writer.println(getPreamble(level) + message);
                if (m_autoFlush && flushThisMessage) {
                    m_writer.flush();
                }
            }
        }
    }

    /**
     * Writes a log message, if the level of the message matches/exceeds initial configuration.
     * Auto-flush is controlled on creation.
     * @param level
     * @param message
     */
    public synchronized void Log(int level, MemoryStream message)
    {
        if (level >= m_min_level)
        {
            synchronized (m_writer)
            {
                m_writer.println(getPreamble(level) + "memory stream length=" + message.getLength() + "\n<<<");
                try {
					m_writer.write(message.getBuffer());
				} catch (IOException e) {
					m_writer.println(DateTimeNowTicks() + " error writing MemoryStream");
				}
                m_writer.println("\n>>>\n" + getPreamble(level) + "end memory stream");

                if (m_autoFlush)
                    m_writer.flush();
            }
        }
    }
    
    private static String DateTimeNowTicks()
    {
    	return  ISO8601DATEFORMAT.format(new Date());
    }
}