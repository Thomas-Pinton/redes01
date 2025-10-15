# Trabalho 1 — Transferência de Arquivos Confiável sobre UDP

## Descrição

Este projeto implementa uma **transferência de arquivos entre cliente e servidor usando UDP**.  
Como o UDP não garante confiabilidade, foram criados mecanismos de **controle, verificação e retransmissão** para tornar a comunicação confiável, simulando parte do que o TCP faz.

---

## Objetivo

- Criar uma aplicação **cliente-servidor** usando **sockets UDP**.  
- Permitir que o cliente solicite arquivos ao servidor.  
- Garantir a **entrega completa e correta dos dados**, mesmo com perdas.  

---

## Funcionamento

### Servidor
- Espera requisições do cliente (`GET /arquivo.ext`);  
- Verifica se o arquivo existe:  
  - Se **não existir**, envia mensagem de erro;  
  - Se **existir**, envia o arquivo dividido em partes (segmentos);  
- Cada segmento contém número de sequência e verificação (checksum ou hash);  
- Reenvia partes perdidas quando o cliente solicitar.

### Cliente
- Envia pedido de arquivo para o servidor;  
- Recebe e ordena os segmentos;  
- Verifica integridade (checksum/hash);  
- Solicita retransmissão se faltar algo;  
- Monta e salva o arquivo completo localmente.  

---

## Protocolo (resumo)

| Tipo de mensagem | Direção | Função |
|------------------|----------|--------|
| `GET /arquivo` | Cliente → Servidor | Solicita arquivo |
| `DATA <seq>` | Servidor → Cliente | Envia parte do arquivo |
| `NACK <seq>` | Cliente → Servidor | Solicita retransmissão |
| `ERROR` | Servidor → Cliente | Arquivo não encontrado |
| `END` | Servidor → Cliente | Fim da transmissão |

---

## Testes Realizados Para Garantir Bom Funcionamento

- Enviar arquivo grande (>10 MB);  
- Simular perda de pacotes e testar retransmissão;  
- Testar pedido de arquivo inexistente;  
- Testar cliente iniciado antes do servidor;  
- Testar interrupção do servidor durante envio.  

