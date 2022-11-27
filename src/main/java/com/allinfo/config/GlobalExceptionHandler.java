package com.allinfo.config;

import com.allinfo.exception.BaseException;
import com.allinfo.exception.ErrorMessage;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validationHandler(MethodArgumentNotValidException e) {
        // TODO: Error Message 리팩토링 필요.
        BaseException baseException = new BaseException(ErrorMessage.VALIDATION_FAIL_EXCEPTION);
        List<ObjectError> messageList = null;
        if (e != null)
            messageList = ((MethodArgumentNotValidException) e).getBindingResult().getAllErrors();
        else
            messageList = ((org.springframework.validation.BindingResult) e).getAllErrors();
        for (int i = 0; i < messageList.size(); i++) {
            String validationMessage = messageList.get(i).getDefaultMessage();
            baseException.appendMsg(validationMessage);
        }
        return new ResponseEntity<Object>(Error.create(baseException), baseException.getHttpStatus());
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<?> handler(Throwable t) {
        t.printStackTrace();
        logger.error(t.getMessage());

        return new ResponseEntity<Object>(new HashMap<String, String>() {{
            put("msg", "알 수 없는 예외입니다.");
        }}, HttpStatus.INTERNAL_SERVER_ERROR);

    }

    @NoArgsConstructor
    @AllArgsConstructor
    static class Error {
        private int code;
        private HttpStatus status;
        private List<String> message;
        private List<String> trace;

        static Error create(BaseException exception) {
            return new Error(exception.getErrorCode(), exception.getHttpStatus(), exception.getErrorMessage(), exception.getErrorTrace());
        }

        public int getCode() {
            return code;
        }

        public HttpStatus getStatus() {
            return status;
        }

        public List<String> getMessage() {
            return message;
        }

        public List<String> getTrace() {
            return trace;
        }
    }
}
