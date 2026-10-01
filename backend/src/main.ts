import 'reflect-metadata';
import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { readEnvironment } from './config/environment';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.enableShutdownHooks();
  app.useGlobalPipes(new ValidationPipe({ transform: true, whitelist: true, forbidNonWhitelisted: true }));
  const swaggerConfig = new DocumentBuilder()
    .setTitle('Estoque API')
    .setDescription('API de consulta de estoque e transferência entre lojas.')
    .setVersion('0.1.0')
    .build();
  SwaggerModule.setup('docs', app, () =>
    SwaggerModule.createDocument(app, swaggerConfig),
  );
  await app.listen(readEnvironment().port, '127.0.0.1');
}

bootstrap().catch(() => {
  // Nest registra a falha de inicialização. Não imprimir configuração/credenciais.
  process.exitCode = 1;
});
