import { join } from 'node:path';
import { DataSourceOptions } from 'typeorm';
import { readEnvironment } from '../config/environment';

export function databaseOptions(): DataSourceOptions {
  return {
    type: 'postgres',
    ...readEnvironment().database,
    entities: [join(__dirname, '../**/*.entity{.ts,.js}')],
    migrations: [join(__dirname, 'migrations/*{.ts,.js}')],
    synchronize: false,
    migrationsRun: false,
    logging: false,
    connectTimeoutMS: 5000,
    extra: { statement_timeout: 5000 },
  };
}
