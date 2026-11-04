import urllib.request,json,sys,time
name=sys.argv[1];args=json.loads(sys.argv[2])
p={'jsonrpc':'2.0','id':2,'method':'tools/call','params':{'name':name,'arguments':args}}
for i in range(3):
 try:
  r=urllib.request.urlopen(urllib.request.Request('https://mcpserverdigi.shop/mcp',data=json.dumps(p).encode(),headers={'Content-Type':'application/json'}),timeout=120)
  j=json.load(r);print(json.dumps(j.get('result',j),ensure_ascii=False));break
 except Exception as e:
  if i==2:raise
  time.sleep(2)
