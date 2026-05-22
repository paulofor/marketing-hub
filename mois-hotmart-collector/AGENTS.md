


**Leia do documento : /docs/canonical/mois-hotmart-mapeamento-ciclos-campos-banco.md**

Exemplo para acessar dados de produtos na hotmart depois de logado e usando o token:

depois que vc consegue o token pode acessar por aqui :

fetch("https://api-affiliation-market.hotmart.com/v2/market/search", {\n  "headers": {\n    "accept": "application/json, text/plain, */*",\n    "accept-language": "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6",\n    "authorization": "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJUR1QtNzM4NzQtaXMyTDRPMlZZNjN0NTU0djZEVGItT2EtMllkcHU4R2NrcEV6Z3JzRFRWZzZlREg2TGFHOUMtOHdJMlBUUi1aNWl2by1ob3Qtc3NvLTVmNGY5ZGY5NTUtOXE5ZngiLCJzaWQiOiIxNmFhOTZmOS1kMzIzLTRkOWItYjZiMi1lNjY5ZDczY2M3NjMiLCJpc3MiOiJodHRwczovL3Nzby5ob3RtYXJ0LmNvbS9vaWRjIiwiYXVkIjpbImZiNWUxOGJhLTIwM2YtMTFlYS05NzhmLTJlNzI4Y2U4ODEyNSIsIjhjZWYzNjFiLTk0ZjgtNDY3OS1iZDkyLTlkMWNiNDk2NDUyZCJdLCJleHAiOjE3Nzg2OTE0MTksImlhdCI6MTc3ODUxODIxOSwibmJmIjoxNzc4NTE3OTE5LCJzdWIiOiI2MTAyMzEyOSIsImFtciI6WyJIb3RtYXJ0Q3JlZGVudGlhbHNBdXRoZW50aWNhdGlvbkhhbmRsZXIiXSwiY2xpZW50X2lkIjoiOGNlZjM2MWItOTRmOC00Njc5LWJkOTItOWQxY2I0OTY0NTJkIiwiYXV0aF90aW1lIjoxNzc4NTE4MjE3LCJzdGF0ZSI6Ijg1MWUxNTM3N2Q4YjQ0ZmVhNDY0NjA4MzZkNmM3MWVmIiwiYXRfaGFzaCI6IktrellVWlluUklkSjF2NFNpVGI4T0EiLCJhZGRyZXNzIjp7ImNvdW50cnkiOiJCcmFzaWwiLCJpZCI6NzYwNjAyNjV9LCJhZGRyZXNzQ291bnRyeSI6IkJyYXNpbCIsImFkZHJlc3NJZCI6NzYwNjAyNjUsImF1dGhvcml0aWVzIjpbInZlbmRlZG9yIiwicHJvZHV0b3IiLCJhbmFseXRpY3NfbmV3X2V0bHNfdXNlcnMiLCJIT1RQUk9fRlJFRU1JVU0iLCJkaXNwbGF5IiwiY29tcHJhZG9yIiwiV0VCSU5BUiIsIkhPVE1BUlRfTElWRV9CRVRBIiwiUFJJVkFDWV9EUEEiLCJQTEFURk9STV9TSUdOVVAiLCJ1c2VyX2JyIl0sImN1cnJlbmN5Q29kZUNvbWlzc2lvbiI6IlVTRCIsImVtYWlsIjoicGF1bG9mb3JlQGdtYWlsLmNvbSIsImVudGl0eVR5cGUiOiJJTkRJVklEVUFMX0VOVElUWSIsImlkIjoiNjEwMjMxMjkiLCJsb2NhbGUiOiJQVF9CUiIsImxvZ2luIjoicGF1bG9mb3JlIiwibG9naW5BdHRlbXB0cyI6MCwibmFtZSI6IlBBVUxPIEFMRVhBTkRSRSIsInNpZ251cERhdGUiOjE2NzExNTQwNTAwMDAsInN0YXR1cyI6IkF0aXZvIiwidHJhZGVOYW1lIjoiQW1hbmRhIERlY2siLCJ1Y29kZSI6ImI1NTA2N2YzLTlmZGUtNDRiOC1iNjQzLTZjMTJmZDk1ZmUyNSIsInByZWZlcnJlZF91c2VybmFtZSI6IjYxMDIzMTI5Iiwic2NvcGUiOlsidXNlciIsImF1dGhvcml0aWVzIiwiZW1haWwiLCJvcGVuaWQiLCJwcm9maWxlIl0sImFjY2Vzc190b2tlbiI6IkFULTEzMjA2NC1VallLRWNZN3dvRVF3R2ZMNi1NbC1aV3dkSnp6cXNIVSJ9.o92qtw9VeVaxrCOT3aV-UHUt1169ijSlmltVQdtiiAhEIllIgdcV2teQfjjsHXBg2YgrzVQ1QrNkeOuqfg7bNi6rVVCl5479baWJVzqI478Mp9JWarsKQdZGrVTOeuNq7_0yO4aC3h_cF63gHRHzap063Pyk3qJYH24F-LjxrgsjlAKdL18ym25CsS12s_HgAWqO7bQpQsAwDjd2VwkQT31A4dhwTDvnWVCWPnc9PUTYb_Y0eK7HXxCg7A__51hoe3jEDLgvBuxdRYD3lLfcuLpE9eS2Nx61yuNbd7k_f9pvn8w7IVoXr3sOzNkP_cXqafGpl9VrUIOe1Teud_Bdlg",\n    "content-type": "application/json",\n    "priority": "u=1, i",\n    "sec-ch-ua": "\"Google Chrome\";v=\"147\", \"Not.A/Brand\";v=\"8\", \"Chromium\";v=\"147\"",\n    "sec-ch-ua-mobile": "?0",\n    "sec-ch-ua-platform": "\"Windows\"",\n    "sec-fetch-dest": "empty",\n    "sec-fetch-mode": "cors",\n    "sec-fetch-site": "same-site",\n    "x-app-name": "[object Object]"\n  },\n  "referrer": "https://app.hotmart.com/",\n  "body": "{\"page\":1,\"rows\":20,\"userLocale\":\"PT_BR\",\"name\":\"hottest\",\"userSessionId\":\"GA1.1.2080780581.1777238477_f0fa388a-dcb8-4b45-a64d-0395b476aa8a\",\"hasSendEvent\":true}",\n  "method": "POST",\n  "mode": "cors",\n  "credentials": "include"\n});


# Se precisar criar endpoints no backeend crie no coletor do mois

## Regra obrigatória de logs em integrações OpenAI (semelhante ao Gera Landing)
- Sempre que o MOIS executar uma requisição para a OpenAI, registrar log com:
  - envio para a OpenAI contendo **request cru** + **jobId do Marketing Hub**;
  - resposta da OpenAI contendo **resposta crua** + **jobId do Marketing Hub**;
  - envio para o backend contendo **payload enviado** + **jobId do Marketing Hub**.
