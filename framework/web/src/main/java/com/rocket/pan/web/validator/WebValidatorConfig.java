package com.rocket.pan.web.validator;

import com.rocket.pan.core.constants.RPanConstants;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.HibernateValidator;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;


/**
 * 统一参数校验器
 *
 * @author 19750
 * @version 1.0
 */
@SpringBootConfiguration
@Slf4j
public class WebValidatorConfig {
    private static final String FAIL_FAST_KEY = "hibernate.validator.fail_fast";

    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        // 后置参数处理器，在方法执行时会判断参数是否符合我们的预期，如果符合预期就会做直接的拦截
        MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
        postProcessor.setValidator(rPanValidator());
        log.info("The hibernate validator is loaded successfully!");
        return postProcessor;
    }

    /**
     * 构造项目的方法参数校验器
     *
     * @return 方法参数校验器
     */
    private Validator rPanValidator() {
        // 工厂模式
        ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class)
                .configure()
                .addProperty(FAIL_FAST_KEY, RPanConstants.TRUE_STR)
                .buildValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        return validator;
    }
}
