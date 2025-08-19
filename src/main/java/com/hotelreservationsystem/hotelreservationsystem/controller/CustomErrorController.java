package com.hotelreservationsystem.hotelreservationsystem.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        System.err.println("=== CUSTOM ERROR CONTROLLER CALLED ===");
        System.err.println("Status Code: " + status);
        System.err.println("Exception: " + exception);
        System.err.println("Message: " + message);
        System.err.println("Request URI: " + requestUri);
        System.err.println("Request URL: " + request.getRequestURL());
        System.err.println("Query String: " + request.getQueryString());
        System.err.println("Method: " + request.getMethod());
        System.err.println("Referer: " + request.getHeader("Referer"));
        System.err.println("User-Agent: " + request.getHeader("User-Agent"));
        System.err.println("Content-Type: " + request.getContentType());
        System.err.println("Context Path: " + request.getContextPath());
        System.err.println("Servlet Path: " + request.getServletPath());
        
        // Log all request parameters
        System.err.println("Request Parameters:");
        if (request.getParameterMap().isEmpty()) {
            System.err.println("  (No parameters)");
        } else {
            request.getParameterMap().forEach((key, values) -> {
                System.err.println("  " + key + ": " + String.join(", ", values));
            });
        }
        
        // Log all headers
        System.err.println("Request Headers:");
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            System.err.println("  " + headerName + ": " + request.getHeader(headerName));
        });
        
        // Print stack trace if exception exists
        if (exception instanceof Exception) {
            System.err.println("Exception Stack Trace:");
            ((Exception) exception).printStackTrace();
        }
        
        System.err.println("=== END ERROR DETAILS ===");
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            if (statusCode == 404) {
                model.addAttribute("errorTitle", "Page Not Found");
                model.addAttribute("errorMessage", "The page you're looking for doesn't exist.");
            } else if (statusCode == 403) {
                model.addAttribute("errorTitle", "Access Denied");
                model.addAttribute("errorMessage", "You don't have permission to access this resource.");
            } else if (statusCode == 500) {
                model.addAttribute("errorTitle", "Internal Server Error");
                model.addAttribute("errorMessage", "Something went wrong on our end. Please try again.");
            } else {
                model.addAttribute("errorTitle", "Error " + statusCode);
                model.addAttribute("errorMessage", message != null ? message.toString() : "An unexpected error occurred.");
            }
            
            model.addAttribute("statusCode", statusCode);
        } else {
            model.addAttribute("errorTitle", "Unknown Error");
            model.addAttribute("errorMessage", "An unexpected error occurred.");
            model.addAttribute("statusCode", "Unknown");
        }
        
        model.addAttribute("requestUri", requestUri);
        
        return "error";
    }
}