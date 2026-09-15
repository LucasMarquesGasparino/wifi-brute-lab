# Wi-Fi Brute Lab

APK **didático e 100% offline** para demonstrar busca exaustiva **sem realizar
nenhuma tentativa de conexão**. Ideal para aula de algoritmos/complexidade.

- Pacote: `com.faculdade.wifibrutelab`

## Garantias de segurança

- **Não declara permissões** de Wi-Fi, Internet ou localização.
- Não enumera SSIDs e não chama APIs de conexão.
- O "nome da rede" é somente um rótulo exibido na tela.
- A senha-alvo fica na memória durante a execução e não é persistida.
- Espaço limitado a **3 milhões** de candidatos (trava contra uso excessivo).

## Experimento

Configure identificador fictício (ex.: `LAB-WIFI-01`), senha-alvo, alfabeto
(ex.: `abc123`) e comprimento máximo (1–8). O app percorre candidatos em ordem
lexicográfica por tamanho e mostra telemetria ao vivo: tentativas, tempo,
taxa/s, % do espaço e resultado.

## Ideia para o trabalho

Para alfabeto de tamanho `b` e comprimento máximo `m`, o pior caso é
`b + b² + … + bᵐ` = `O(bᵐ)`. Compare taxa e tempo ao variar `b` ou `m`.

## Compilar

```sh
cd ~/projects/wifi-brute-lab
./build.sh
```

O APK assinado sai em `build/WiFi-Brute-Lab.apk`.
