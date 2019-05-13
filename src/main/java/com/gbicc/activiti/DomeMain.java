package com.gbicc.activiti;

import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * 启动类
 * Created with IntelliJ IDEA.
 *
 * @program: activiti6-dev
 * @create: 2019-05-12 09:11
 * @Author: monstar..lzq
 **/
public class DomeMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomeMain.class);

    public static void main(String[] args) throws ParseException {

        LOGGER.info("启动程序");
        // 创建流程引擎
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration
                .createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String processEngineName = processEngine.getName();
        String processEngineVersion = processEngine.VERSION;
        LOGGER.info("流程引擎的名称 [{}] ，流程引擎的版本 [{}]", processEngineName, processEngineVersion);

        //部署流程定义文件
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("second_approve.bpmn20.xml");
        Deployment deployment = deploymentBuilder.deploy();
        String deploymentId = deployment.getId();
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
        LOGGER.info("流程定义文件 [{}] ，流程ID [{}] ，流程KEY [{}]", processDefinition.getName(), processDefinition.getId(), processDefinition.getKey());

        //启动运行流程
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        LOGGER.info("启动流程 [{}]，[{}] ", processInstance.getProcessDefinitionKey(), processInstance.getProcessDefinitionId());

        //处理流程任务
        Scanner scanner = new Scanner(System.in);
        while (processInstance != null && !processInstance.isEnded()) {

            TaskService taskService = processEngine.getTaskService();
            List<Task> taskList = taskService.createTaskQuery().list();
            LOGGER.info("待处理的任务数 [{}]", taskList.size());
            HashMap<String, Object> map = Maps.newHashMap();
            for (Task task : taskList) {
                LOGGER.info("待处理的任务 [{}]", task.getName());
                LOGGER.info("======================================");
                FormService formService = processEngine.getFormService();
                TaskFormData taskFormData = formService.getTaskFormData(task.getId());
                List<FormProperty> formProperties = taskFormData.getFormProperties();
                for (FormProperty formProperty : formProperties) {
                    String line = "";
                    if (StringFormType.class.isInstance(formProperty.getType())) {
                        LOGGER.info("请输入 {} ?", formProperty.getName());
                        line = scanner.nextLine();
                        map.put(formProperty.getId(), line);
                    } else if (DateFormType.class.isInstance(formProperty.getType())) {
                        LOGGER.info("请输入 {} ? 格式：(yyyy-MM-dd)", formProperty.getName());
                        line = scanner.nextLine();
                        while (!isValidDate(line)) {
                            LOGGER.info("您的输入有误，请按照要求输入！[{}]", line);
                            LOGGER.info("请输入 {} ? 格式：(yyyy-MM-dd)", formProperty.getName());
                            line = scanner.nextLine();
                        }
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date date = simpleDateFormat.parse(line);
                        map.put(formProperty.getId(), date);
                    } else {
                        LOGGER.info("类型暂不支持 {}", formProperty.getType());
                    }
                    LOGGER.info("您输入的内容是 [{}]", line);
                    LOGGER.info("======================================");
                }
                // 提交任务
                taskService.complete(task.getId(), map);
                // 获取当前流程最新的状态
                processInstance = processEngine.getRuntimeService()
                        .createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .singleResult();
            }
        }
        LOGGER.info("结束程序");
    }

    private static boolean isValidDate(String str) {
        boolean convertSuccess = true;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            format.setLenient(false);
            format.parse(str);
        } catch (ParseException e) {
            // 格式不对
            convertSuccess = false;
        }
        return convertSuccess;
    }
}
