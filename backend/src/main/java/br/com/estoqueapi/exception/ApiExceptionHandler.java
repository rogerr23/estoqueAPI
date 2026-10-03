package br.com.estoqueapi.exception;

import br.com.estoqueapi.dto.ErroResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NegocioException.class)
    ResponseEntity<ErroResponse> negocio(NegocioException error) {
        return ResponseEntity.status(error.getStatus()).body(new ErroResponse(error.getCodigo(), error.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException error) {
        var campos = new LinkedHashMap<String, String>();
        error.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErroResponse("DADOS_INVALIDOS", "Revise os campos informados", campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErroResponse> formato(Exception error) {
        return ResponseEntity.badRequest().body(new ErroResponse("DADOS_INVALIDOS", "Formato de dados inválido"));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ErroResponse> ausente(Exception error) {
        return ResponseEntity.status(404).body(new ErroResponse("RECURSO_NAO_ENCONTRADO", "Recurso não encontrado"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErroResponse> metodo(Exception error) {
        return ResponseEntity.status(405).body(new ErroResponse("METODO_NAO_PERMITIDO", "Método não permitido"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErroResponse> midia(Exception error) {
        return ResponseEntity.status(415).body(new ErroResponse("FORMATO_NAO_SUPORTADO", "Tipo de conteúdo não suportado"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResponse> interno(Exception error) {
        log.error("Falha interna ao processar requisição", error);
        return ResponseEntity.internalServerError().body(new ErroResponse("ERRO_INTERNO", "Não foi possível concluir a operação"));
    }
}
