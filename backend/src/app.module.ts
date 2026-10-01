import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { databaseOptions } from './database/database.options';
import { HealthController } from './health/health.controller';
import { ProdutosModule } from './produtos/produtos.module';

@Module({
  imports: [
    ProdutosModule,
    TypeOrmModule.forRootAsync({
      useFactory: () => ({ ...databaseOptions(), retryAttempts: 3, retryDelay: 1000 }),
    }),
  ],
  controllers: [HealthController],
})
export class AppModule {}
