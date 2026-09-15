import urllib.request,json,sys,time,pathlib

def call(name,args):
 for attempt in range(3):
  try:
   data=json.dumps({'jsonrpc':'2.0','id':1,'method':'tools/call','params':{'name':name,'arguments':args}}).encode()
   r=urllib.request.urlopen(urllib.request.Request('https://mcpserverdigi.shop/mcp',data=data,headers={'Content-Type':'application/json','Accept':'application/json, text/event-stream'}),timeout=150)
   j=json.load(r)
   return j
  except Exception as e:
   if attempt==2:raise
   time.sleep(2)
if __name__=='__main__':
 result=call(sys.argv[1],json.loads(sys.argv[2])); print(json.dumps(result,ensure_ascii=False))
