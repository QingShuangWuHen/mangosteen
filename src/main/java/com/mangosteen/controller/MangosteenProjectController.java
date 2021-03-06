package com.mangosteen.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mangosteen.model.ExecuteRecords;
import com.mangosteen.model.Project;
import com.mangosteen.model.ProjectConfig;
import com.mangosteen.service.JacocoService;
import com.mangosteen.service.MangosteenUserService;
import com.mangosteen.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;


@Controller
@RequestMapping("/project")
@Slf4j
public class MangosteenProjectController {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private JacocoService jacocoService;
    @Autowired
    private MangosteenUserService mangosteenUserService;
    @Value("${mangosteen.workspace}")
    private String mangosteenWorkSpace;
    private int pageSize=15;


    @RequestMapping("/showProject")
    public String createProject(HttpServletRequest httpServletRequest, ModelMap modelMap) {

        List<Project> projectList = projectService.queryAllProject();
        modelMap.addAttribute("templatename", "projectList");
        modelMap.addAttribute("projectList", projectList);

        return "index";
    }

    @RequestMapping("/delelteProject")
    public String delelteProject(HttpServletRequest httpServletRequest, ModelMap modelMap) {

        String projectName = httpServletRequest.getParameter("projectName");
        projectService.deleteByProjectName(projectName);
        List<Project> projectList = projectService.queryAllProject();
        modelMap.addAttribute("templatename", "projectList");
        modelMap.addAttribute("projectList", projectList);

        return "index";
    }
    /**
     * 跳转到添加项目页面
     *
     * @param httpServletRequest
     * @param modelMap
     * @return
     */
    @RequestMapping("/toAddProject")
    public String toAddProject(HttpServletRequest httpServletRequest, ModelMap modelMap) {
        modelMap.addAttribute("templatename", "addProject");
        return "index";
    }

    @RequestMapping("/login")
    public String toLogin(HttpServletRequest httpServletRequest, ModelMap modelMap) {
        return "login";
    }



    @RequestMapping("/logout")
    public String logout(HttpServletRequest httpServletRequest) {
        httpServletRequest.getSession().removeAttribute("user");
        return "login";
    }


    @RequestMapping("/isLogin")
    @ResponseBody
    public String login(HttpServletRequest httpServletRequest){

        String userName=httpServletRequest.getParameter("userName");
        String passwd=httpServletRequest.getParameter("password");
        boolean isLogin = mangosteenUserService.isLogin(userName, passwd);
        if(isLogin){
            httpServletRequest.getSession().setAttribute("user",mangosteenUserService.queryUserRole(userName));
            return "SUCCESS";
        }
        return "FAIL";

    }


    /**
     * 跳转到项目执行页面
     *
     * @param httpServletRequest
     * @param modelMap
     * @return
     */
    @RequestMapping("/toExecuteProject")
    public String toExecuteProject(HttpServletRequest httpServletRequest, Model modelMap) {
        String projectName = httpServletRequest.getParameter("projectName");
        Project project = projectService.queryProjectByName(projectName);
        List<ProjectConfig> projectConfigs=JSON.parseArray(project.getProjectConfig(), ProjectConfig.class);
        modelMap.addAttribute("project",project);
        modelMap.addAttribute("projectConfig",projectConfigs);
        modelMap.addAttribute("templatename", "executeProject");
        return "index";
    }

    /**
     * 跳转到构建历史记录
     *
     * @param httpServletRequest
     * @param modelMap
     * @return
     */
    @RequestMapping("/tobuildHistory")
    public String tobuildHistory(HttpServletRequest httpServletRequest, Model modelMap) {
        String projectName = httpServletRequest.getParameter("projectName");
        modelMap.addAttribute("templatename", "buildHistory");
        int currentPage=StringUtils.isBlank(httpServletRequest.getParameter("currentPage"))?1: Integer.parseInt(httpServletRequest.getParameter("currentPage"));
        int pageCount=projectService.countExecuteRecordByProjectName(projectName);
        int pageTime=pageCount%pageSize==0?pageCount/pageSize:pageCount/pageSize+1;
        List<ExecuteRecords> executeRecords=projectService.queryExecuteRecordByProjectName(projectName,(currentPage-1)*pageSize,pageSize);
        modelMap.addAttribute("executeRecords",executeRecords);
        modelMap.addAttribute("pageCount",pageCount);
        modelMap.addAttribute("pageTime",pageTime);
        modelMap.addAttribute("projectName",projectName);
        return "index";
    }

    @RequestMapping("/histroylog")
    public  @ResponseBody List<String> historyLog(HttpServletRequest httpServletRequest) {
        List<String> list=null;
        String logPath = httpServletRequest.getParameter("logPath");
        String logFile = mangosteenWorkSpace+"/"+StringUtils.substringBefore(logPath, "coveragereport/")+"build.log" ;
        try {
            list= FileUtils.readLines(new File(logFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }
    /**
     * 跳转到构建历史记录
     *
     * @param httpServletRequest
     * @param modelMap
     * @return
     */
    @RequestMapping("/queryHistory")
    public String queryHistory(HttpServletRequest httpServletRequest, Model modelMap) {
        String projectName = httpServletRequest.getParameter("projectName");
        String beginTime=httpServletRequest.getParameter("beginTime");
        String endTime=httpServletRequest.getParameter("endTime");
        String branch=httpServletRequest.getParameter("branch").trim();
        int currentPage=StringUtils.isBlank(httpServletRequest.getParameter("currentPage"))?1: Integer.parseInt(httpServletRequest.getParameter("currentPage"));
        int pageCount=projectService.countExecuteRecordByProjectNameAndBranch(projectName,branch);
        int pageTime=pageCount%pageSize==0?pageCount/pageSize:pageCount/pageSize+1;
        List<ExecuteRecords> executeRecords=projectService.queryExecuteRecordByProjectName(projectName,branch,beginTime,endTime,(currentPage-1)*pageSize,pageSize);
        modelMap.addAttribute("executeRecords",executeRecords);
        modelMap.addAttribute("pageCount",pageCount);
        modelMap.addAttribute("pageTime",pageTime);
        modelMap.addAttribute("templatename", "buildHistory");
        modelMap.addAttribute("projectName",projectName);
        modelMap.addAttribute("branch",branch);
        return "index";
    }



    @RequestMapping("/saveProject")
    @ResponseBody
    public  String addProject(@RequestBody String request) {
            JSONObject requestData = JSON.parseObject(request);
            Project project=new Project();
            project.setProjectName(requestData.getString("projectName"));
            project.setCodeBranch(requestData.getString("codeBranch"));
            project.setProjectConfig(requestData.getJSONArray("projectConfig").toString());
            projectService.saveProject(project);
        return "SUCCESS";
    }

    @RequestMapping("/updateProject")
    @ResponseBody
    public  String updateProject(@RequestBody String request) {
        JSONObject requestData = JSON.parseObject(request);
        Project project=new Project();
        project.setProjectName(requestData.getString("projectName"));
        project.setCodeBranch(requestData.getString("codeBranch"));
        project.setId(requestData.getInteger("projectId"));
        project.setProjectConfig(requestData.getJSONArray("projectConfig").toString());
        projectService.updateProjectById(project);
        return "SUCCESS";
    }
    @RequestMapping(value = "/reportDetail")
    public String toReport(HttpServletRequest httpServletRequest, Model modelMap) {

        modelMap.addAttribute("templatename", "jacocoReportDetail");
        modelMap.addAttribute("reportPath", httpServletRequest.getParameter("reportPath"));
        modelMap.addAttribute("projectName",httpServletRequest.getParameter("projectName"));
        modelMap.addAttribute("codeBranch",httpServletRequest.getParameter("codeBranch"));
        modelMap.addAttribute("serverIp",httpServletRequest.getParameter("serverIp"));

        return "index";
    }

    @RequestMapping("/buildReport")
    public String buildReport(HttpServletRequest request,Model modelMap){
        JSONObject requestData = JSON.parseObject(request.getParameter("buildReportForm"));
        ExecuteRecords executeRecords=new ExecuteRecords();
        executeRecords.setProjectName(requestData.getString("hf-projectName"));
        Project project = projectService.queryProjectByName(executeRecords.getProjectName());
        String codeBranch=requestData.getString("hf-devBranch");
        if(StringUtils.isBlank(codeBranch)){
            codeBranch=project.getCodeBranch();
        }else if (codeBranch.endsWith("/")){
            codeBranch=StringUtils.substringBeforeLast(codeBranch,"/");
        }
        if(project.getCodeBranch().endsWith(".git")){
            executeRecords.setGit(true);
        }
        executeRecords.setCodeBranch(codeBranch);
        executeRecords.setServerIp(requestData.getString("hf-ip").replace("[","").replace("]","").replace("\"",""));
        executeRecords.setExecuteTime(new Date());
        if(StringUtils.isNotBlank(request.getParameter("Increment"))){
            executeRecords.setDiffUrl(project.getCodeBranch());
            executeRecords.setIncrement(true);
        }
        String reportPath = null;
        try {
            reportPath = jacocoService.buildReport(executeRecords);
        } catch (IOException e) {
            log.error("覆盖率报告生成失败:{}",e.getMessage());
            reportPath ="/error.html";
        }
        if (StringUtils.isNotBlank(reportPath)){
            executeRecords.setReportPath(reportPath);
            projectService.saveProjectExecuteRecords(executeRecords);

        }else {
            reportPath ="/error.html";
        }
        modelMap.addAttribute("templatename", "jacocoReportDetail");
        modelMap.addAttribute("reportPath", reportPath);
        modelMap.addAttribute("projectName",executeRecords.getProjectName());
        modelMap.addAttribute("codeBranch",executeRecords.getCodeBranch());
        modelMap.addAttribute("serverIp",executeRecords.getServerIp());

        return "index";
    }

    @RequestMapping("/toConfigProject")
    public String toConfigProject(HttpServletRequest request,ModelMap modelMap){
        String projectName = request.getParameter("projectName");
        Project project = projectService.queryProjectByName(projectName);
        List<ProjectConfig> projectConfigs=JSON.parseArray(project.getProjectConfig(), ProjectConfig.class);
        modelMap.addAttribute("project", project);
        modelMap.addAttribute("projectConfigs", projectConfigs);
        modelMap.addAttribute("templatename", "updateProject");

        return "index";
    }



    @RequestMapping("/toResetCoverage")
    public String toResetCoverage(HttpServletRequest request,ModelMap modelMap){
        List<Project> projectList = projectService.queryAllProject();
        modelMap.addAttribute("templatename", "resetCoverage");
        modelMap.addAttribute("projectList", projectList);
        return "index";
    }

    @RequestMapping("/resetCoverage")
    @ResponseBody
    public String resetCoverage(HttpServletRequest request){
        String ip=request.getParameter("ip");
        boolean resetDump = jacocoService.resetDump(StringUtils.substringBefore(ip,":"), StringUtils.substringAfter(ip,":"));
        if(resetDump){
            return "SUCCESS";
        }
        return "FAIL";
    }

    @RequestMapping("/queryServerIP")
    @ResponseBody
    public List<ProjectConfig> queryServerIP(HttpServletRequest request){
        String projectName = request.getParameter("projectName");
        Project project = projectService.queryProjectByName(projectName);
        List<ProjectConfig> projectConfigs=JSON.parseArray(project.getProjectConfig(), ProjectConfig.class);

        return projectConfigs;
    }



}
