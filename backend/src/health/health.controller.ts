import { Controller, Get, ServiceUnavailableException } from '@nestjs/common';
import { DataSource } from 'typeorm';
import { ApiOkResponse, ApiOperation, ApiServiceUnavailableResponse, ApiTags } from '@nestjs/swagger';

@ApiTags('Saúde')
@Controller('health')
export class HealthController {
  constructor(private readonly database: DataSource) {}

  @Get()
  @ApiOperation({ summary: 'Verificar a conexão da API com PostgreSQL' })
  @ApiOkResponse({
    description: 'API conectada ao banco de dados.',
    schema: {
      type: 'object',
      required: ['status', 'database'],
      properties: {
        status: { type: 'string', enum: ['ok'], example: 'ok' },
        database: { type: 'string', enum: ['up'], example: 'up' },
      },
    },
  })
  @ApiServiceUnavailableResponse({
    description: 'Banco de dados indisponível.',
    schema: {
      type: 'object',
      required: ['statusCode', 'message', 'error'],
      properties: {
        statusCode: { type: 'integer', example: 503 },
        message: { type: 'string', example: 'Banco de dados indisponível' },
        error: { type: 'string', example: 'Service Unavailable' },
      },
    },
  })
  async check() {
    try {
      await this.database.query('SELECT 1');
      return { status: 'ok', database: 'up' };
    } catch {
      throw new ServiceUnavailableException('Banco de dados indisponível');
    }
  }
}
