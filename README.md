# Trabalho 2 - Construção de Compiladores (2024/1)

## Integrantes do grupo
- Arisa Abiko Sakaguti - 800357
- Matheus Goulart Ranzani - 800278
- Thales Winther - 802499

## Contexto
O trabalho 3 (T3) da disciplina consistiu em implementar um analisador semântico para a linguagem LA (Linguagem Algorítmica) 
desenvolvida pelo prof. Jander, no âmbito do DC/UFSCar. O analisador semântico é capaz de ler um programa-fonte e indificar onde existe
um erro semântico, indicando a linha e o lexema que causou a detecção do erro

## Documentação externa

### Pré-requisitos
- Java instalado na versão 11 ou superior
- Alguma IDE capaz de buildar e compilar o projeto
- Algum terminal que consiga executar comandos Java
- Casos de teste para testar o analisador que podem ser baixados [aqui](https://drive.google.com/file/d/1FD9GZm_ECGLcVNLcqIP7fkF2T9coe8hs/view)

### Compilação
Para o desenvolvimento do trabalho foi utilizada o Apache NetBeans na versão IDE 21.\
Abra o projeto através da IDE e defina o SDK do projeto como um SDK do Java 11 ou superior, caso ainda não esteja definido.\
No menu superior acesse a seção de `Build` e realize o build do projeto.
Se o build for concluído com sucesso já é possível criar o arquivo .jar do analisador ou executar localmente o programa através da classe `Principal.java`.

Para compilar e executar o analisador pela própria IDE é preciso seguir as etapas:
- No menu superior acessar e cliclar com o botão direito no diretório raiz do projeto
- Clicar em "Clean and Build"
- Definir a seção de `Program arguments` com os dois argumentos do programa separados por vírgula, que são:
  - Argumento 1: arquivo de entrada (caminho completo)
  - Argumento 2: arquivo de saída (caminho completo)
- Por fim, basta clicar no botão `Run` e se tudo correr como o esperado o programa analisará o Argumento 1 e criará a sáida como sendo o Argumento 2

### Execução
Para a execução do analisador semântico em uma linha de comando, siga as seguintes instruções para os respectivos sistemas operacionais:

#### Linux
Em Linux é recomendável extrair os arquivos de `casos-de-teste.zip` baixados para a home do seu usuário, assim como criar uma pasta `temp` no mesmo diretório para armazenar as saídas geradas.\
Também é possível mover o arquivo `AnalisadorSemantico.jar` para outro caminho que não seja o do projeto.\
Com o ambiente configurado, basta rodar o seguinte comando e se tudo correr bem um arquivo de saída será gerado na pasta `temp`:

``java -jar /caminho_absoluto/AnalisadorAnalisadorSemantico.jar /home/usuario/casos-de-teste/3.casos_teste_t3/entrada/entrada1.txt /home/usuario/temp/saida1.txt``

#### Windows
No Windows o processo é muito semelhante ao do Linux, sendo recomendável colocar a pasta `casos-de-teste` na raíz do seu sistema, juntamento com a pasta `temp`, onde serão salvas as saídas.\
Sabendo onde está cada arquivo basta executar o seguinte comando no terminal:

``java -jar c:\caminho_absoluto\AnalisadorLexico.jar c:\casos-de-teste\3.casos_teste_t3\entrada\entrada2.txt c:\temp\saida2.txt``

Para cada saída gerada é possível compará-la com a saída esperada acessando a pasta `casos-de-teste/3.casos_teste_t3/saida`.
