# Fluxo de pré-análise e aprovação de licenças

## Objetivo
Preparar o Reprodutor Inteligente para um fluxo de compra com conferência manual do Pix e aprovação definitiva pelo administrador.

## Regras comerciais acordadas
- Preço: R$ 9,99 por compra única.
- Após aprovação, o acesso é por tempo indeterminado.
- Não implementar transferência de licença entre aparelhos nesta etapa.
- O pagamento é conferido manualmente pelo administrador antes da liberação definitiva.

## Fluxo do cliente
1. O cliente solicita acesso pelo botão de atendimento.
2. O sistema pode emitir um código provisório de solicitação, válido por 24 horas.
3. Esse código provisório não libera o uso completo; apenas identifica a solicitação e permite acompanhar a análise.
4. O cliente envia o comprovante Pix em PDF ou imagem.
5. A pré-análise extrai, quando possível, valor, data/hora, identificador da transação e dados visíveis do pagador.
6. A análise procura inconsistências, baixa qualidade, indícios de edição e possível repetição do mesmo comprovante.
7. O resultado é apenas indicativo: aparentemente consistente, suspeito ou incompleto.
8. O administrador confere o recebimento real no RecargaPay e aprova ou rejeita.
9. Somente após aprovação administrativa o app recebe a licença definitiva, vinculada à instalação/aparelho.
10. A licença aprovada funciona offline por tempo indeterminado.

## Painel administrativo
- Aplicativo separado do player.
- Criar senha de administrador no primeiro acesso, com configuração segura; nunca embutir senha no APK ou no repositório.
- Listar solicitações pendentes, horário de envio e resultado da pré-análise.
- Permitir abrir o comprovante enviado e visualizar os dados extraídos.
- Ações explícitas: aprovar, rejeitar e emitir novo código provisório.
- Gerar código provisório aleatório com expiração de 24 horas.
- Registrar trilha de auditoria das decisões administrativas.
- Não aprovar automaticamente com base apenas em PDF, imagem, OCR ou análise de IA.

## Segurança e limites técnicos
- Um comprovante em PDF/imagem pode ser falsificado ou reutilizado. A pré-análise não confirma liquidação do Pix nem entrada de dinheiro na conta.
- A aprovação definitiva exige conferência manual do crédito no RecargaPay.
- Se o servidor rodar no celular do administrador, ele precisa estar ligado, com internet e acessível durante a solicitação/ativação. CGNAT e bloqueios da operadora podem exigir túnel seguro.
- Não guardar dados bancários ou comprovantes além do necessário; restringir acesso e permitir exclusão conforme política de retenção.
- Os códigos provisórios expiram após 24 horas e não devem ser confundidos com licenças definitivas.
- A licença definitiva deve ser assinada criptograficamente pelo emissor e validada pelo player; a chave privada nunca pode ficar no APK do cliente.
- Sem conexão com o servidor, uma nova ativação ou aprovação não pode ser concluída. Após receber uma licença válida, o player poderá operar offline.
- Não prometer proteção absoluta contra cópia, adulteração do aparelho ou alteração do relógio local.

## Fora do escopo desta etapa
- Integração automática com a API do RecargaPay.
- Aprovação automática de pagamentos.
- Transferência de licença entre aparelhos.
- Renovação ou assinatura recorrente.