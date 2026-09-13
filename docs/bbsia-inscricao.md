# Inscrição no BBSIA — Patrimônio Inteligente

Respostas ao formulário **Compartilhar uma solução ou ideia** do BBSIA — Banco Brasileiro de
Soluções de IA para a Gestão Pública (LIIA/ENAP), em <https://bancobrasileiro.ia.br/contribuir>.

O cadastro original foi feito em agosto de 2026 e o rascunho se perdeu (ficou sem versionar).
Este arquivo é a reconstrução, alinhada ao README de 13/09/2026, e passa a ser a fonte para
qualquer atualização do cadastro. O gabarito genérico, para inscrever outros projetos, está fora
deste repositório (`OpenSource/BBSIA/_kit/formulario-bbsia.md`).

> Tudo o que vai no formulário é público. Nada aqui é sigiloso — mas os dados de contato pessoal
> (e-mail, telefone, cargo) ficam fora deste arquivo de propósito.

---

## Bloco 1 — Identificação e localização

| Campo | Resposta |
|---|---|
| E-mail institucional* | *(preencher no ato — não versionar)* |
| Nome completo* | André Maia |
| Cargo | *(preencher no ato)* |
| WhatsApp / telefone | *(opcional — não versionar)* |
| Órgão / Entidade / Empresa* | Prefeitura de São José dos Campos – SP · Departamento de Planejamento e Gestão de Recursos (DPGR) |
| Nível de governo* | Municipal |
| Estado (UF)* | SP |
| Cidade / Município* | São José dos Campos |

## Bloco 2 — A solução

**Nome curto da solução\***
Patrimônio Inteligente

**Em uma frase, qual problema ela resolve?\*** *(sem termos técnicos)*
O inventário dos bens públicos ainda é feito com prancheta e caneta, placa por placa, leva meses
e já nasce desatualizado; a solução lê as etiquetas dos bens com o celular (ou com um leitor de
rádio de longo alcance) para que a conferência leve dias e o cadastro pare de se descolar da
realidade.

**Como ela funciona?** *(opcional)*
Um aplicativo Android lê a identificação do bem de três formas: pelo código de barras da
plaqueta (câmera), por etiqueta NFC (aproximando o celular) ou, para varredura de salas inteiras
sem encostar em nada, por RFID UHF através de um pequeno leitor (ESP32 + módulo UHF) que conversa
com o celular por Bluetooth. Cada leitura vira um registro (código + descrição) numa lista de
inventário sem duplicidade, pronta para conferência e, no futuro, para integração com o sistema de
patrimônio do órgão. Os modos código de barras e NFC funcionam hoje só com o celular; o modo UHF
está em validação de bancada.

**Tecnologia utilizada** *(opcional)*
Não usa IA *(hoje)*.
Justificativa: o README declara com todas as letras que o projeto atual é captura de dado (IoT e
automação). A camada de IA está especificada como roadmap — reconciliação de identidade e lotação,
OCR de placa patrimonial, enquadramento contábil assistido, detecção de anomalias — e depende
exatamente do dado contínuo que o RFID passa a gerar. Marcar "Previsão / classificação" hoje
contradiria a seção "Estado real do projeto", que é o principal ativo de credibilidade do
repositório. Detalhar nas Observações.

**Tipo de ativo\***
Código ou biblioteca para instalar e adaptar
*(aplicativo Android em Kotlin + firmware ESP32, ambos com fonte aberta; não é um sistema pronto
para instalar em produção)*

**Área de atuação\***
Gestão Pública
*(alternativa: Administração/Processos — a dor é controle patrimonial, transversal a qualquer
órgão)*

## Bloco 3 — Maturidade

**Além de você ou da sua equipe, alguém já usou?\***
Só nós

**Em que ponto está hoje?\***
Ainda em desenvolvimento (protótipo)
*(prova de conceito validada em hardware real; sem uso em rotina de inventário)*

## Bloco 4 — Abertura e soberania

**É aberta / reusável por outro órgão?\***
Sim, código aberto
*(código Apache 2.0; documentação CC BY 4.0; licença permissiva escolhida para remover atrito de
adoção por jurídicos municipais e fornecedores)*

**Desenvolvida majoritariamente com recursos públicos?**
Sim
*(iniciativa própria do departamento, com recurso mínimo — cerca de R$ 1.200 em hardware de
bancada)*

**Roda em modelo aberto e infra nacional, ou depende de serviço externo?**
Infra/modelo nacional ou aberto
*(o aplicativo roda no próprio aparelho, sem serviço externo, sem Google Play Services e sem
enviar dado a terceiros; a restrição de não depender de serviço externo é requisito de projeto,
inclusive para a futura camada de IA)*

**Lida com dado pessoal ou sensível?**
Não
*(trata número patrimonial, EPC da etiqueta, descrição e localização do bem — dados do órgão,
não de pessoas)*

## Bloco 5 — Fechamento

**Link(s) da solução\***
- Repositório: <https://github.com/maia-andre/patrimonio-inteligente>
- Documentação técnica (spec, plano, relatórios de build/verify, bancada do leitor UHF):
  <https://github.com/maia-andre/patrimonio-inteligente/tree/main/docs>

**Tem algum número ou resultado pra contar?**
Ainda não há resultado de campo — o projeto está em fase de protótipo. Os números que existem
são do problema, não da solução: o acervo de um município de porte médio-grande passa de 180 mil
bens, espalhados por centenas de unidades; conferir uma sala com ~30 postos de trabalho consome em
torno de 7,5 servidor-horas (três pessoas, 2–3 h), e o inventário completo mobiliza centenas de
servidores por vários meses. A meta do piloto é medir tempo e precisão por sala com leitura
eletrônica contra esse baseline.

**Sua instituição está disposta a fornecer a solução para ser adaptada e escalada como código
aberto?\***
SIM
*(já está publicada sob Apache 2.0; o repositório tem CONTRIBUTING e uma seção "Procuram-se
parceiros")*

**Observações e sugestões**
- Hoje não há IA no projeto — é captura de dado (IoT). Cadastramos porque a agenda de IA sobre
  patrimônio só existe se existir dado de patrimônio, e o RFID é o sensor que transforma o
  inventário de evento raro em fluxo contínuo. As quatro aplicações de IA previstas estão
  especificadas no README ("Onde entra a Inteligência Artificial") com problema, entrada → saída
  e métrica, como convite a colaboradores.
- O README mantém uma seção "Estado real do projeto" com o que funciona, o que está simulado e o
  que não existe. Quem for avaliar adoção deve começar por ela.
- Os modos código de barras e NFC funcionam sem nenhum hardware além de um Android. O modo RFID
  UHF depende de um módulo leitor (~R$ 1.200), recebido em 05/09/2026 e em validação de bancada.
- Procuramos: outros municípios com a mesma dor; órgãos que já tenham leitor UHF de outro modelo
  (para comparar e portar o firmware); universidades para caracterização de RFID, OCR de placas e
  reconciliação de entidades; e quem já publicou software público, para orientar o trâmite de
  autorização institucional.

**Li e concordo com o aviso de privacidade\*** — ☑
