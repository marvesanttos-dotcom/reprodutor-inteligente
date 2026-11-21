# Estado da implementação do Painel de Ativação

Este módulo é uma primeira base técnica para execução local no Android. Ele não deve ser usado para receber clientes ou liberar licenças de produção até que as pendências abaixo sejam resolvidas e testadas.

## O que esta versão contém
- Módulo Android separado `:painel`.
- Serviço HTTP local na porta 8080, executado como serviço em primeiro plano.
- Cadastro de senha administrativa com hash PBKDF2-HMAC-SHA256 e salt aleatório.
- Criação de solicitação com código aleatório provisório de 24 horas.
- Recebimento de PDF/JPEG/PNG e cálculo SHA-256 para detectar arquivo idêntico já enviado.
- Classificação básica do arquivo como incompleto ou suspeito; não faz OCR confiável nem confirma dados bancários.
- Lista de solicitações e ações de aprovação/rejeição protegidas pela senha administrativa.
- Workflow preparado para compilar o APK do player e o APK do painel.

## Pendências obrigatórias antes de produção
- O player ainda não está integrado aos endpoints para solicitar código, enviar comprovante e validar/guardar uma licença aprovada.
- A aprovação atual gera um token aleatório de demonstração, mas ainda não é uma licença criptograficamente assinada e validável offline. Não entregar esse token como licença de produção.
- O painel ainda não possui interface completa para abrir/visualizar os arquivos recebidos.
- A pré-análise atual só reconhece cabeçalhos de arquivo e duplicidade binária; não extrai valor, data, pagador ou identificador do Pix.
- O serviço escuta em HTTP na porta 8080. Não exponha essa porta diretamente à internet. Acesso externo exige túnel HTTPS com autenticação e configuração cuidadosa.
- O app ainda precisa de limites de taxa, proteção contra tentativas de senha, política de retenção/exclusão dos comprovantes, auditoria e testes de segurança.
- Android pode encerrar serviços; é necessário testar economia de bateria, reinicialização e conectividade da operadora.

## Fluxo previsto
1. Cliente solicita código provisório; validade de 24 horas.
2. Cliente envia PDF ou imagem.
3. Painel marca duplicidade/tipo de arquivo, sem afirmar que o Pix foi pago.
4. Administrador confere o crédito real de R$ 9,99 no RecargaPay.
5. Administrador aprova ou rejeita.
6. Somente após implementação da assinatura criptográfica e integração com o player a licença definitiva poderá ser emitida por tempo indeterminado e funcionar offline.

## Como testar a base local
- Instale o APK do Painel apenas em aparelho de teste.
- Crie uma senha forte com pelo menos 10 caracteres.
- Inicie o servidor e confirme `/health` na rede local.
- Não use comprovantes reais durante os testes iniciais.
- Não encaminhe a porta 8080 para a internet sem túnel HTTPS e revisão de segurança.

## Atualização de compatibilidade
- A senha administrativa usa PBKDF2-HMAC-SHA1 para compatibilidade com Android API 23, com 210.000 iterações e comparação em tempo constante.
- O upload bloqueia um segundo comprovante para o mesmo código provisório.
- Formatos aceitos nesta versão: PDF, JPEG e PNG.
