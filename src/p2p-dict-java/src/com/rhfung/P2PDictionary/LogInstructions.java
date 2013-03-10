package com.rhfung.P2PDictionary;

//P2PDictionary
//Copyright (C) 2013, Richard H Fung (www.richardhfung.com)
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
import java.util.Calendar;
import com.rhfung.Interop.MemoryStream;


class LogInstructions
{
    private PrintStream m_writer;
    int m_min_level = 0;
    boolean m_autoFlush = false;

    public LogInstructions(OutputStream writer, int min_level_to_log, boolean autoFlush)
    {
        m_writer = new PrintStream(writer);
        m_min_level = min_level_to_log;
        m_autoFlush = false;
    }

    public PrintStream GetTextWriter()
    {
        return m_writer;
    }

    /// <summary>
    /// Writes a log message, if the level of the message matches/exceeds initial configuration.
    /// Auto-flush is controlled on creation.
    /// </summary>
    /// <param name="level">level of the message</param>
    /// <param name="message"></param>
    public void Log(int level, String message, boolean flushThisMessage)
    {
        if (level >= m_min_level)
        {
        	synchronized (m_writer)
            {
                m_writer.println(DateTimeNowTicks() + "t [" + level + "] "  + message);
                if (m_autoFlush && flushThisMessage)
                    m_writer.flush();
            }
        }
    }

    /// <summary>
    /// Writes a log message, if the level of the message matches/exceeds initial configuration.
    /// Auto-flush is controlled on creation.
    /// </summary>
    /// <param name="level">level of the message</param>
    /// <param name="message"></param>
    public void Log(int level, MemoryStream message)
    {
        if (level >= m_min_level)
        {
            synchronized (m_writer)
            {
                m_writer.println(DateTimeNowTicks() + "t [" + level + "] " + " memory stream length " + message.getLength());
                try {
					m_writer.write(message.getBuffer());
				} catch (IOException e) {
					m_writer.println(DateTimeNowTicks() + " error writing MemoryStream");
				}
                m_writer.println(DateTimeNowTicks() + "t [" + level + "] " + " end memory stream");

                if (m_autoFlush)
                    m_writer.flush();
            }
        }
    }
    
    private String DateTimeNowTicks()
    {
    	return String.valueOf(Calendar.getInstance().getTimeInMillis());
    }
}