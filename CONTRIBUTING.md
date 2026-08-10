# Como contribuir

Obrigado pelo interesse. Este projeto nasceu de uma necessidade concreta de um município brasileiro, e existe em aberto porque **a mesma dor se repete em centenas de órgãos públicos**. Toda contribuição que reduza esse retrabalho coletivo é bem-vinda.

Antes de qualquer coisa: **você não precisa escrever código para contribuir.** As contribuições mais valiosas para este projeto, hoje, não são pull requests.

---

## O que mais precisamos, em ordem de urgência

### 1. 🔬 Acesso a um leitor RFID UHF

O projeto está bloqueado por uma compra de aproximadamente R$ 1.200 travada em trâmite administrativo. O firmware que conversaria com o módulo YRM100 já está escrito.

Se o seu órgão, universidade ou empresa já tem um leitor UHF disponível, **você pode destravar esta etapa em uma tarde**. [Abra uma issue](../../issues/new) contando qual módulo você tem e ajudamos a rodar.

### 2. 📋 Relatos de campo — inclusive de fracasso

Se você tentou usar isto e não funcionou, **queremos saber mais do que se funcionou**. RFID UHF é uma tecnologia sensível ao ambiente: metal e líquidos degradam severamente a leitura, e um almoxarifado de estantes metálicas se comporta de forma muito diferente de uma sala administrativa.

Relate: qual módulo, qual antena, qual etiqueta, qual ambiente, qual alcance obtido, o que quebrou. Esse tipo de dado é o mais escasso e o mais caro de produzir.

### 3. 🏛️ Validação por quem entende de patrimônio público

Se você trabalha com patrimônio, contabilidade pública ou controle interno, sua revisão vale mais que otimização de código. Especificamente:

- O modelo de dados faz sentido para o seu fluxo de inventário?
- A aderência ao MCASP e às NBC TSP está correta?
- Que campo indispensável no seu órgão nós ignoramos?

### 4. ⚖️ Orientação sobre publicação de software público

Estamos formalizando a autorização institucional de publicação (veja [NOTICE](NOTICE)). Se você já passou por esse trâmite em outro órgão, sua experiência nos poupa meses.

### 5. 💻 Código

Veja o [roadmap no README](README.md#️-roadmap). Itens de curto prazo não dependem do hardware bloqueado e podem ser tocados imediatamente. A camada de IA está especificada e completamente livre.

---

## Antes de abrir um Pull Request

1. **Abra uma issue primeiro** para mudanças não triviais. É um projeto pequeno e mantido por servidor público em tempo limitado — alinhar antes evita trabalho jogado fora.
2. **Uma mudança por PR.** Facilita revisão e reversão.
3. **Descreva como testou.** Especialmente em firmware: informe placa, versão da IDE e o que observou no Serial Monitor.

## Padrões do projeto

**Geral**
- Código, comentários, commits e documentação **em português**. Este projeto é lido por servidores públicos brasileiros, e a barreira de idioma é uma barreira de adoção real.
- Preferimos **clareza didática a elegância**. Alguém em outra prefeitura vai ler isto tentando aprender BLE. Comentário explicando o "porquê" é bem-vindo.
- Sem overengineering. Não introduza uma camada de abstração para um caso de uso que ainda não existe.

**Firmware (C++ / Arduino)**
- Compatível com a Arduino IDE, sem exigir PlatformIO ou toolchain adicional.
- Responsabilidades separadas por arquivo (`ble_service`, `led_controller`, ...).
- Logs no Serial Monitor com prefixo de categoria: `[BOOT]`, `[BLE]`, `[RX]`, `[TX]`, `[SCANNER]`.

**Android (Kotlin)**
- Jetpack Compose para a interface; XML tradicional foi removido do projeto.
- Estrutura por responsabilidade: `ble/`, `model/`, `ui/`.
- Alterações no protocolo BLE **devem ser espelhadas** em `BleConstants.kt` e no firmware, e documentadas na seção de protocolo do README.

---

## ⚠️ Nunca inclua dados reais

Este é um projeto de patrimônio público, e o risco é concreto.

**Nunca faça commit de:**
- Placas patrimoniais, códigos EPC ou descrições de bens reais
- Nomes de servidores, matrículas ou qualquer dado pessoal
- Endpoints internos, credenciais, chaves de API ou dumps de banco
- Plantas, endereços ou qualquer informação que exponha a localização física de bens

Use **exclusivamente dados fictícios** em exemplos, testes e capturas de tela. O registro patrimonial usado na simulação atual (placa `147258`, unidade `124`) é inteiramente fictício e serve de modelo do que é aceitável.

Se encontrar algum dado real que tenha escapado no histórico do repositório, **não abra issue pública** — entre em contato de forma reservada.

---

## Licença das contribuições

Ao contribuir, você concorda que sua contribuição será licenciada sob a [Apache License 2.0](LICENSE), nos mesmos termos do restante do projeto. Contribuições em documentação seguem a CC BY 4.0.

---

## Código de conduta

Seja direto e seja respeitoso — as duas coisas ao mesmo tempo. Crítica técnica dura é bem-vinda; desqualificação pessoal não. Muita gente aqui é servidor público aprendendo tecnologia fora da sua formação original, e essa é exatamente a audiência que queremos alcançar.
