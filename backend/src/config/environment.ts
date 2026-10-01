import { config } from 'dotenv';
import { resolve } from 'node:path';

// Funciona tanto em src/config (ts-node) quanto dist/config (build).
config({ path: resolve(__dirname, '../../../.env') });

function required(name: string): string {
  const value = process.env[name];
  if (!value || value.trim().length === 0) {
    throw new Error(`Variável de ambiente obrigatória ausente: ${name}`);
  }
  return value;
}

function port(name: string, fallback: string): number {
  const value = process.env[name] ?? fallback;
  if (!/^\d+$/.test(value) || Number(value) < 1 || Number(value) > 65535) {
    throw new Error(`${name} deve ser um número inteiro entre 1 e 65535`);
  }
  return Number(value);
}

export function readEnvironment() {
  const nodeEnv = process.env.NODE_ENV ?? 'development';
  if (!['development', 'test', 'production'].includes(nodeEnv)) {
    throw new Error('NODE_ENV deve ser development, test ou production');
  }
  return {
    nodeEnv,
    port: port('PORT', '3000'),
    database: {
      host: required('DB_HOST'),
      port: port('DB_PORT', '5432'),
      username: required('DB_USER'),
      password: required('DB_PASSWORD'),
      database: required('DB_NAME'),
    },
  };
}
