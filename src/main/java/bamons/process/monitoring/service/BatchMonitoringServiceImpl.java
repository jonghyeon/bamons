/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/david100gom/Bamons
 * <p/>
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * <p/>
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * <p/>
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package bamons.process.monitoring.service;

import bamons.process.monitoring.dao.BatchMonitoringDAO;
import bamons.process.monitoring.domain.BatchJobExecution;
import bamons.process.monitoring.domain.BatchStepExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

@Service
public class BatchMonitoringServiceImpl implements BatchMonitoringService{

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("restoreFileJob")
    private Job restoreFileJob;

    @Autowired
    private BatchMonitoringDAO batchMonitoringDAO;

    /**
     *
     * Job 리스트 정보을 가져온다.
     *
     * @return List<BatchJobExecution> 객체
     * @throws Exception
     */
    @Override
    public List<BatchJobExecution> findBatchJobExecutionList(String start, String limit, String startTime) throws Exception {
        return batchMonitoringDAO.findBatchJobExecutionList(start, limit, startTime);
    }

    /**
     *
     * Job 리스트 총 카운트
     *
     * @return 총 카운트
     * @throws Exception
     */
    @Override
    public int getBatchJobExecutionTotalCount(String startTime) throws Exception {
        return batchMonitoringDAO.getBatchJobExecutionTotalCount(startTime);
    }

    /**
     *
     * Job & Step 데이터
     *
     * @param jobExecutionId JOB ID
     * @return Map<String, Object> 객체
     * @throws Exception
     */
    @Override
    public Map<String, Object> findBatchJobStepExecutionData(int jobExecutionId) throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();

        // JOB 정보
        map.put("jobInfo", batchMonitoringDAO.getBatchJobExecutionInfo(jobExecutionId));
        // STEP 정보
        map.put("stepInfo", batchMonitoringDAO.findBatchStepExecutionList(jobExecutionId));

        return map;
    }

    /**
     *
     * Step 데이터
     *
     * @param jobExecutionId JOB ID
     * @return Map<String, Object> 객체
     * @throws Exception
     */
    @Override
    public List<BatchStepExecution> findBatchStepExecutionList(int jobExecutionId) throws Exception {
        return batchMonitoringDAO.findBatchStepExecutionList(jobExecutionId);
    }

    /**
     *
     * Job 을 재구동한다.
     *
     * @param jobExecutionId Job ID
     * @param jobName Job 이름
     * @throws Exception
     */
    @Override
    public void jobRestart(int jobExecutionId, String jobName) throws Exception {

        try {

            String params = jobOperator.getParameters(jobExecutionId);

            String[] paramValues = new String[2];
            String targetDate = "";
            String filePath = "";

            StringTokenizer st = new StringTokenizer(params, ",");
            while(st.hasMoreTokens()) {
                String sd = st.nextToken();
                if(sd.contains("targetDate")) {
                    paramValues = sd.split("=");
                    targetDate = paramValues[1];
                    break;
                }
                if(sd.contains("filePath")) {
                    paramValues = sd.split("=");
                    filePath = paramValues[1];
                    break;
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("time(long)=");
            sb.append(System.currentTimeMillis());

            if(!StringUtils.isEmpty(targetDate)) {
                sb.append(",targetDate=");
                sb.append(targetDate);
            }else if(!StringUtils.isEmpty(filePath)) {
                sb.append(",filePath=");
                sb.append(filePath);
            }

            jobOperator.start(jobName, sb.toString());

        }catch (Exception e) {
            throw e;
        }

    }

    /**
     *
     * Job 을 구동한다.
     *
     * @param jobName Job 이름
     * @param targetDate 통계날짜
     * @throws Exception
     */
    public void jobStart(String jobName, String targetDate) throws Exception {

        try {

            StringBuilder sb = new StringBuilder();
            sb.append("time(long)=");
            sb.append(System.currentTimeMillis());
            sb.append(",targetDate=");
            sb.append(targetDate);

            jobOperator.start(jobName, sb.toString());

        }catch (Exception e) {
            throw e;
        }

    }

    /**
     *
     * Job 을 중지한다.
     *
     * @param jobExecutionId Job ID
     * @throws Exception
     */
    @Override
    public void jobStop(int jobExecutionId) throws Exception {
        try {
            jobOperator.stop(jobExecutionId);
        }catch (Exception e) {
           throw e;
        }
    }

    /**
     *
     * rawdata (json file)를 restore 한다.
     *
     * @param filePath 파일 경로
     * @throws Exception
     */
    @Async
    @Override
    public void restoreRawData(String filePath) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis()).addString("filePath", filePath).toJobParameters();
        JobExecution execution = jobLauncher.run(restoreFileJob, jobParameters);
        execution.getJobId();
    }
}