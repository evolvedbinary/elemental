/*
 * Elemental
 * Copyright (C) 2024, Evolved Binary Ltd
 *
 * admin@evolvedbinary.com
 * https://www.evolvedbinary.com | https://www.elemental.xyz
 *
 * Use of this software is governed by the Business Source License 1.1
 * included in the LICENSE file and at www.mariadb.com/bsl11.
 *
 * Change Date: 2028-04-27
 *
 * On the date above, in accordance with the Business Source License, use
 * of this software will be governed by the Apache License, Version 2.0.
 *
 * Additional Use Grant: Production use of the Licensed Work for a permitted
 * purpose. A Permitted Purpose is any purpose other than a Competing Use.
 * A Competing Use means making the Software available to others in a commercial
 * product or service that: substitutes for the Software; substitutes for any
 * other product or service we offer using the Software that exists as of the
 * date we make the Software available; or offers the same or substantially
 * similar functionality as the Software.
 *
 * NOTE: Parts of this file contain code from 'The eXist-db Authors'.
 *       The original license header is included below.
 *
 * =====================================================================
 *
 * eXist-db Open Source Native XML Database
 * Copyright (C) 2001 The eXist-db Authors
 *
 * info@exist-db.org
 * http://www.exist-db.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.xquery.functions.system;


import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.exist.dom.QName;
import org.exist.storage.BrokerPool;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.NumericValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;

/**
 * Shutdown the eXist server (must be dba)
 * 
 * @author Adam Retter (adam.retter@devon.gov.uk)
 */
public class Shutdown extends BasicFunction
{
	protected final static Logger logger = LogManager.getLogger(Shutdown.class);

	public final static FunctionSignature[] signatures = {
		new FunctionSignature(
			new QName("shutdown", SystemModule.NAMESPACE_URI, SystemModule.PREFIX),
			"Shutdown eXist immediately.  This method is only available to the DBA role.",
			null,
			new SequenceType(Type.EMPTY_SEQUENCE, Cardinality.EMPTY_SEQUENCE)
		),
		
		new FunctionSignature(
			new QName("shutdown", SystemModule.NAMESPACE_URI, SystemModule.PREFIX),
			"Shutdown eXist.  This method is only available to the DBA role.",
			new SequenceType[] {
					new FunctionParameterSequenceType("delay", Type.LONG, Cardinality.EXACTLY_ONE, "The delay in milliseconds before eXist starts to shutdown.")
			},
			new SequenceType(Type.EMPTY_SEQUENCE, Cardinality.EMPTY_SEQUENCE)
		)
	};
		

	public Shutdown(XQueryContext context, FunctionSignature signature)
	{
		super(context, signature);
	}

	/* (non-Javadoc)
	 * @see org.exist.xquery.BasicFunction#eval(org.exist.xquery.value.Sequence[], org.exist.xquery.value.Sequence)
	 */
	public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException
	{
		if(context.getSubject().hasDbaRole())
		{
			//determine the shutdown delay
			long delay = 0;
			if(args.length == 1)
			{
				if(!args[0].isEmpty())
				{
					delay = ((NumericValue)args[0].itemAt(0)).getLong();
				}
			}
			
			//get the broker pool and shutdown
			final BrokerPool pool = context.getBroker().getBrokerPool();
				
			if(delay > 0)
			{
				logger.info("Shutdown in {} milliseconds.", delay);
				final Timer timer = new Timer("Elemental shutdown schedule", true);
				final TimerTask task = new DelayedShutdownTask(timer, pool);
				timer.schedule(task, delay);
			}
			else
			{
				logger.info("Shutting down now.");
				pool.shutdown();
			}
		}
		else
		{
			final XPathException xPathException = new XPathException(this, "Permission denied, calling user '" + context.getSubject().getName() + "' must be a DBA to shutdown the database");
			logger.error("Invalid user", xPathException);
			throw xPathException;
		}
			
		return Sequence.EMPTY_SEQUENCE;
	}
	
	private static class DelayedShutdownTask extends TimerTask {
		private Timer timer;
		private BrokerPool pool;
		
		public DelayedShutdownTask(final Timer timer, final BrokerPool pool) {
			this.timer = timer;
			this.pool = pool;
		}

		@Override
		public void run() {
			logger.info("Shutting down now.");
			pool.shutdown();

			// make sure to stop the timer thread!
			timer.cancel();

			timer = null;
			pool = null;
		}
	}
}
