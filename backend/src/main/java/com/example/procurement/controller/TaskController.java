package com.example.procurement.controller;

import com.example.procurement.application.dto.TaskCompletionRequest;
import com.example.procurement.application.dto.TaskDetailResponse;
import com.example.procurement.application.dto.TaskSummaryResponse;
import com.example.procurement.camunda.service.TaskApplicationService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Task façade used by React for all human workflow transitions (plan sections 18, 34.3, 42.2-42.4). */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskApplicationService taskApplicationService;

    public TaskController(TaskApplicationService taskApplicationService) {
        this.taskApplicationService = taskApplicationService;
    }

    @GetMapping
    public List<TaskSummaryResponse> search(
            @RequestParam(required = false) String candidateGroup,
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly) {
        return taskApplicationService.search(candidateGroup, activeOnly);
    }

    @GetMapping("/{taskKey}")
    public TaskDetailResponse getTask(@PathVariable long taskKey) {
        return taskApplicationService.getTask(taskKey);
    }

    @PostMapping("/{taskKey}/claim")
    public void claim(@PathVariable long taskKey, Principal principal) {
        taskApplicationService.claim(taskKey, principal != null ? principal.getName() : "demo-user");
    }

    @PostMapping("/{taskKey}/complete")
    public void complete(
            @PathVariable long taskKey, @RequestBody TaskCompletionRequest request, Principal principal) {
        taskApplicationService.complete(taskKey, request, principal != null ? principal.getName() : "demo-user");
    }
}
