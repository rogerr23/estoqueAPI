package br.com.estoqueapi.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.*;
import io.swagger.v3.oas.models.responses.*;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI estoqueOpenApi() {
        var components = new Components().addSecuritySchemes("sessao",
            new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID"));
        components.addSchemas("UsuarioSessao", new ObjectSchema()
            .addProperty("id", new IntegerSchema().format("int64"))
            .addProperty("nome", new StringSchema()).addProperty("email", new StringSchema()));
        components.addSchemas("ErroApi", new ObjectSchema()
            .addProperty("codigo", new StringSchema()).addProperty("mensagem", new StringSchema())
            .addProperty("campos", new MapSchema().additionalProperties(new StringSchema())));
        var csrf = new Parameter().name("X-CSRF-TOKEN").in("header").required(true)
            .description("Obtenha em GET /api/auth/csrf; renove após login e logout")
            .schema(new StringSchema());
        var login = new Operation().summary("Iniciar sessão com e-mail e senha")
            .addTagsItem("Autenticação").security(java.util.List.of())
            .addParametersItem(csrf)
            .requestBody(new RequestBody().required(true).content(new Content().addMediaType(
                "application/x-www-form-urlencoded", new MediaType().schema(new ObjectSchema()
                    .addProperty("email", new StringSchema()).addProperty("senha", new StringSchema().format("password"))
                    .addRequiredItem("email").addRequiredItem("senha")))))
            .responses(new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("Sessão iniciada").content(new Content().addMediaType("application/json", new MediaType().schema(new Schema<>().$ref("#/components/schemas/UsuarioSessao")))))
                .addApiResponse("401", new ApiResponse().description("Credenciais inválidas"))
                .addApiResponse("403", new ApiResponse().description("CSRF ausente ou inválido")));
        var logout = new Operation().summary("Encerrar sessão").addTagsItem("Autenticação")
            .addParametersItem(csrf).responses(new ApiResponses()
                .addApiResponse("204", new ApiResponse().description("Sessão encerrada"))
                .addApiResponse("401", new ApiResponse().description("Sem autenticação"))
                .addApiResponse("403", new ApiResponse().description("CSRF ausente ou inválido")));
        return new OpenAPI().info(new Info().title("EstoqueAPI").version("0.2.0")
                .description("API acadêmica. Login por formulário; sessão e CSRF gerenciados pelo Spring Security."))
            .components(components).addSecurityItem(new SecurityRequirement().addList("sessao"))
            .path("/api/auth/login", new PathItem().post(login))
            .path("/api/auth/logout", new PathItem().post(logout));
    }
    @Bean
    org.springdoc.core.customizers.OpenApiCustomizer contratosDeErro() {
        return api -> api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
            if (!path.equals("/api/auth/csrf") && !path.equals("/api/auth/login")) {
                operation.getResponses().addApiResponse("401", erro("Autenticação necessária"));
            }
            operation.getResponses().addApiResponse("500", erro("Falha interna sem detalhes sensíveis"));
            if (method == PathItem.HttpMethod.POST) {
                operation.getResponses().addApiResponse("403", erro("Token CSRF ausente ou inválido"));
            }
            if (path.equals("/api/auth/login")) operation.getResponses().addApiResponse("401", erro("Credenciais inválidas"));
            if (path.startsWith("/api/produtos") || path.startsWith("/api/lojas")) {
                operation.getResponses().addApiResponse("400", erro("Dados inválidos"));
                if (path.contains("{id}")) operation.getResponses().addApiResponse("404", erro("Recurso inexistente"));
            }
            if (path.equals("/api/produtos") && method == PathItem.HttpMethod.POST) {
                operation.addParametersItem(new Parameter().name("X-CSRF-TOKEN").in("header").required(true).schema(new StringSchema()));
                operation.getResponses().addApiResponse("409", erro("Código duplicado"));
            }
        }));
    }

    private ApiResponse erro(String descricao) {
        return new ApiResponse().description(descricao).content(new Content().addMediaType("application/json",
            new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErroApi"))));
    }
}
