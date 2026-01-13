package com.pgrdaw.tagfolio.controller.advice;

import com.pgrdaw.tagfolio.dto.ErrorResponse;
import com.pgrdaw.tagfolio.service.FilterServiceException;
import com.pgrdaw.tagfolio.service.ExifToolService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Global controller advice to handle exceptions across the application.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    private boolean isHtmlRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(Exception ex, HttpStatus status, HttpServletRequest request) {
        if (isHtmlRequest(request)) {
            throw new ResponseStatusException(status, ex.getMessage(), ex);
        }
        return new ResponseEntity<>(new ErrorResponse(ex.getMessage()), status);
    }

    /**
     * Handles {@link IllegalArgumentException}.
     *
     * @param ex      The exception.
     * @param request The HTTP request.
     * @return A {@link ModelAndView} for HTML requests, or a {@link ResponseEntity} for other requests.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        if (isHtmlRequest(request)) {
             ModelAndView mav = new ModelAndView("error");
             mav.addObject("message", ex.getMessage());
             mav.setStatus(HttpStatus.BAD_REQUEST);
             return mav;
        }
        return createErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Handles {@link FilterServiceException}.
     *
     * @param ex      The exception.
     * @param request The HTTP request.
     * @return A {@link ModelAndView} for HTML requests, or a {@link ResponseEntity} for other requests.
     */
    @ExceptionHandler(FilterServiceException.class)
    public Object handleFilterServiceException(FilterServiceException ex, HttpServletRequest request) {
        if (isHtmlRequest(request)) {
             ModelAndView mav = new ModelAndView("error");
             mav.addObject("message", ex.getMessage());
             mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
             return mav;
        }
        return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Handles {@link ExifToolService.ExifToolException}.
     *
     * @param ex      The exception.
     * @param request The HTTP request.
     * @return A {@link ModelAndView} for HTML requests, or a {@link ResponseEntity} for other requests.
     */
    @ExceptionHandler(ExifToolService.ExifToolException.class)
    public Object handleExifToolException(ExifToolService.ExifToolException ex, HttpServletRequest request) {
        if (isHtmlRequest(request)) {
             ModelAndView mav = new ModelAndView("error");
             mav.addObject("message", ex.getMessage());
             mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
             return mav;
        }
        return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Handles {@link AccessDeniedException}.
     *
     * @param ex      The exception.
     * @param request The HTTP request.
     * @return A {@link ModelAndView} for HTML requests, or a {@link ResponseEntity} for other requests.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        if (isHtmlRequest(request)) {
             ModelAndView mav = new ModelAndView("error");
             mav.addObject("message", ex.getMessage());
             mav.setStatus(HttpStatus.FORBIDDEN);
             return mav;
        }
        return createErrorResponse(ex, HttpStatus.FORBIDDEN, request);
    }

    /**
     * Handles {@link NoSuchElementException}.
     *
     * @param ex      The exception.
     * @param request The HTTP request.
     * @return A {@link ModelAndView} for HTML requests, or a {@link ResponseEntity} for other requests.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public Object handleNoSuchElementException(NoSuchElementException ex, HttpServletRequest request) {
        if (isHtmlRequest(request)) {
             ModelAndView mav = new ModelAndView("error");
             mav.addObject("message", ex.getMessage());
             mav.setStatus(HttpStatus.NOT_FOUND);
             return mav;
        }
        return createErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    /**
     * Handles {@link IOException}.
     *
     * @param ex      The exception.
     * @param request The HTTP request.
     * @return A {@link ModelAndView} for HTML requests, or a {@link ResponseEntity} for other requests.
     */
    @ExceptionHandler(IOException.class)
    public Object handleIOException(IOException ex, HttpServletRequest request) {
        if (isHtmlRequest(request)) {
             ModelAndView mav = new ModelAndView("error");
             mav.addObject("message", ex.getMessage());
             mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
             return mav;
        }
        return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Handles {@link ResponseStatusException}.
     *
     * @param ex      The exception.
     * @param request The HTTP request.
     * @return A {@link ModelAndView} for HTML requests, or a {@link ResponseEntity} for other requests.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public Object handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        if (isHtmlRequest(request)) {
            ModelAndView mav = new ModelAndView("error");
            mav.addObject("message", ex.getReason());
            mav.setStatus(ex.getStatusCode());
            return mav;
        }
        return new ResponseEntity<>(new ErrorResponse(ex.getReason()), ex.getStatusCode());
    }

    /**
     * Handles generic {@link Exception}.
     *
     * @param ex      The exception.
     * @param request The HTTP request.
     * @return A {@link ModelAndView} for HTML requests, or a {@link ResponseEntity} for other requests.
     */
    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request) {
        if (isHtmlRequest(request)) {
             ModelAndView mav = new ModelAndView("error");
             mav.addObject("message", ex.getMessage());
             mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
             return mav;
        }
        return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
