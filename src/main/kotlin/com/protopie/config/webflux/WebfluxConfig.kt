package com.protopie.config.webflux

import org.springframework.context.annotation.Configuration
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer

@Configuration
class WebFluxConfig : WebFluxConfigurer {

    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        val pageableResolver = ReactivePageableHandlerMethodArgumentResolver().apply {
            setOneIndexedParameters(true)
        }
        configurer.addCustomResolver(pageableResolver)
    }
}