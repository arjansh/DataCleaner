/**
 * DataCleaner (community edition)
 * Copyright (C) 2013 Human Inference
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.eobjects.datacleaner.monitor.server.job;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.SerializationUtils;
import org.eobjects.analyzer.configuration.InjectionManager;
import org.eobjects.analyzer.descriptors.ComponentDescriptor;
import org.eobjects.analyzer.job.BeanConfiguration;
import org.eobjects.analyzer.lifecycle.LifeCycleHelper;
import org.eobjects.datacleaner.monitor.configuration.TenantContext;
import org.eobjects.datacleaner.monitor.job.ExecutionLogger;
import org.eobjects.datacleaner.monitor.job.JobEngine;
import org.eobjects.datacleaner.monitor.scheduling.model.ExecutionLog;
import org.eobjects.datacleaner.monitor.shared.model.JobIdentifier;
import org.eobjects.datacleaner.repository.RepositoryFile;
import org.eobjects.datacleaner.repository.RepositoryFolder;
import org.eobjects.datacleaner.util.FileFilters;
import org.eobjects.metamodel.util.Action;
import org.springframework.stereotype.Component;

/**
 * A {@link JobEngine} implementation for running custom Java-class based jobs
 */
@Component
public class CustomJobEngine extends AbstractJobEngine<CustomJobContext> {

    public static final String EXTENSION = ".custom.job.xml";

    public CustomJobEngine() {
        super(EXTENSION);
    }

    @Override
    public String getJobType() {
        return "CustomJob";
    }

    @Override
    protected CustomJobContext getJobContext(TenantContext context, RepositoryFile file) {
        final InjectionManager injectionManager = context.getConfiguration().getInjectionManager(null);

        return new CustomJobContext(this, file, injectionManager);
    }

    @Override
    public void executeJob(TenantContext context, ExecutionLog execution, ExecutionLogger executionLogger,
            Map<String, String> variables) throws Exception {
        executionLogger.setStatusRunning();

        final JobIdentifier jobIdentifier = execution.getJob();
        final CustomJobContext jobContext = getJobContext(context, jobIdentifier);

        final CustomJob customJob;
        final ComponentDescriptor<?> descriptor = jobContext.getDescriptor();
        try {
            customJob = (CustomJob) descriptor.newInstance();
        } catch (Exception e) {
            executionLogger.setStatusFailed(this, descriptor, e);
            return;
        }

        executionLogger.log("Succesfully loaded a job instance of type: " + descriptor.getComponentClass().getName());

        final Serializable result;
        try {
            final BeanConfiguration beanConfiguration = jobContext.getBeanConfiguration(customJob);

            final InjectionManager injectionManager = context.getConfiguration().getInjectionManager(null);
            final LifeCycleHelper lifeCycleHelper = new LifeCycleHelper(injectionManager, true);

            lifeCycleHelper.assignProvidedProperties(descriptor, customJob);
            lifeCycleHelper.assignConfiguredProperties(descriptor, customJob, beanConfiguration);
            lifeCycleHelper.initialize(descriptor, customJob);
            executionLogger.log("Succesfully initialized job instance, executing");

            try {
                final CustomJobCallback callback = new CustomJobCallbackImpl(context, executionLogger);
                result = customJob.execute(callback);
                executionLogger.log("Succesfully executed job instance, closing");
            } finally {
                lifeCycleHelper.close(descriptor, customJob);
            }

        } catch (Exception e) {
            executionLogger.setStatusFailed(this, null, e);
            return;
        }

        if (result != null) {
            try {
                executionLogger.log("Saving job result.");
                serializeResult(result, context, execution);
            } catch (Exception e) {
                executionLogger
                        .log("Failed to save job result! Execution of the job was succesfull, but the result was not persisted.");
                executionLogger.setStatusFailed(null, result, e);
                return;
            }
        }
        executionLogger.setStatusSuccess(result);
    }

    private void serializeResult(final Serializable result, TenantContext context, ExecutionLog execution) {
        if (result == null) {
            return;
        }

        final RepositoryFolder resultFolder = context.getResultFolder();
        final String resultFilename = execution.getResultId() + FileFilters.ANALYSIS_RESULT_SER.getExtension();

        resultFolder.createFile(resultFilename, new Action<OutputStream>() {
            @Override
            public void run(OutputStream out) throws Exception {
                SerializationUtils.serialize(result, out);
            }
        });
    }
}
