# Fabric Trusted Third Party



## 1 系统管理

### 1.1 管理员登录



首先，通过`/api/v1/organization/login`接口登录管理员账户。管理员的账户名默认为`admin`、密码默认为`5NjLjNZQUSTKKoS0`。

```json
{
  "organizationName": "admin",
  "password": "5NjLjNZQUSTKKoS0"
}
```

返回一个用于身份验证的Java Web Token：

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJleHAiOjE2NTAzODk2NjQsImF1dGhvcml0aWVzIjoiUk9MRV9BRE1JTiIsInVzZXJuYW1lIjoiYWRtaW4ifQ.7Mr9n16ndkhoFLkGu0T8Hzn34IAe-yG0k0ncqeH6FhSprySwAfNfj_YjChxFocsD3dJGhiLLGJHWjBdIcgQ9nA"
  }
}
```

token用于身份验证，需要将其设置为Http请求Header的`Authorization`字段来取得服务端的访问权限。



### 1.2 系统初始化

在系统投入使用之前必须先通过`/api/v1/system/init`初始化系统，主要是设置CA服务的参数并启动相应的容器。

```json
{
  "adminPassword": "12345678",
  "ttp": {
    "countryCode": "CN",
    "domain": "ttp.example.com",
    "locality": "Guilin",
    "organizationName": "GXNU",
    "stateOrProvince": "Guangxi"
  }
}
```

初始化系统完成后，系统即可开始正常投入使用。



## 2 组织管理

### 2.1 申请注册组织

使用TTP服务端的组织需要先通过`/api/v1/organization/applyRegistration`来申请注册一个账号。

```json
{
  "apiServer": "orga.example.com:8080",
  "description": "This is an organization for testing ttp server",
  "email": "13833117950@163.com",
  "organizationName": "TestOrgA",
  "password": "12345678"
}
```

```json
{
  "apiServer": "orgb.example.com:8080",
  "description": "This is an organization for testing ttp server",
  "email": "609882524@qq.com",
  "organizationName": "TestOrgB",
  "password": "12345678"
}
```

```json
{
  "apiServer": "orgc.example.com:8080",
  "description": "This is an organization for testing ttp server",
  "email": "1447250889@qq.com",
  "organizationName": "TestOrgC",
  "password": "12345678"
}
```

```json
{
  "apiServer": "orgd.example.com:8080",
  "description": "This is an organization for testing ttp server",
  "email": "kid.1447250889@live.com",
  "organizationName": "TestOrgD",
  "password": "12345678"
}
```

在申请被处理前不可以再进行申请。



### 2.2 处理组织注册申请

管理员首先通过`/api/v1/organization/queryRegistrations`来查询当前的申请信息（status为-1、0、1依次表示已拒绝、待处理、已通过）。

```json
{
  "page": 1,
  "pageSize": 10,
  "status": 0
}
```

返回所有待处理的申请：

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "totalPages": 1,
    "items": [
      {
        "organizationName": "TestOrgC",
        "email": "arseneyao@qq.com",
        "apiServer": "orgc.example.com:8080",
        "description": "This is an organization for testing ttp server",
        "status": 0,
        "timestamp": 1650982685542
      },
      {
        "organizationName": "TestOrgB",
        "email": "arseneyao@foxmail.com",
        "apiServer": "orgb.example.com:8080",
        "description": "This is an organization for testing ttp server",
        "status": 0,
        "timestamp": 1650982620227
      },
      {
        "organizationName": "TestOrgA",
        "email": "1374399100@qq.com",
        "apiServer": "orga.example.com:8080",
        "description": "This is an organization for testing ttp server",
        "status": 0,
        "timestamp": 1650982610197
      }
    ]
  }
}
```

然后通过`/api/v1/organization/handleRegistration`来对指定的申请进行处理（处理的结果将通过邮件发送至相应的邮箱）：

```json
{
  "allowed": true,
  "organizationName": "TestOrgA"
}
```

一个组织在被拒绝申请后仍然可以重复进行申请，直至成功。成功后即可通过`/api/v1/organization/login`登录服务端（用申请的账户）。



### 2.3 查询已注册的组织

通过`/api/v1/organization/queryOrganizations`可以按照组织名称关键词查询到所有已注册的组织的信息。

```json
{
  "organizationNameKeyword": "TestOrg",
  "page": 1,
  "pageSize": 10
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "totalPages": 1,
    "items": [
      {
        "name": "TestOrgA",
        "email": "1374399100@qq.com",
        "apiServer": "orga.example.com:8080"
      },
      {
        "name": "TestOrgB",
        "email": "arseneyao@foxmail.com",
        "apiServer": "orgb.example.com:8080"
      },
      {
        "name": "TestOrgC",
        "email": "arseneyao@qq.com",
        "apiServer": "orgc.example.com:8080"
      }
    ]
  }
}
```



## 3 网络管理

### 3.1 创建网络

首先需要调用`/api/v1/network/createNetwork`接口来创建一个网络，TTP端将生成网络的所有Orderer证书和用于启动Orderer的创世区块。下面假设以组织FabricOrgA的身份创建一个网络：

```json
{
  "consortiumName": "TestConsortium",
  "networkName": "TestNetwork",
  "orderers": [
    {
      "host": "orga.example.com",
      "port": 30500
    },
    {
      "host": "orga.example.com",
      "port": 30501
    }
  ]
}
```

除了上面的内容之外还要附上一个由FabricOrgA组织端CA服务提供的管理员证书的zip格式压缩包。

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "downloadUrl": "/download/block/84961ffb-1850-4833-bc70-13273d55b3fa.block"
  }
}
```

根据返回的Url即可下载到网络的创世区块。此外，还需要通过`/api/v1/network/queryOrdererCert`接口来获取该网络中的Orderer的证书。注意需要Orderer所属的组织才能下载

```json
{
  "networkName": "TestNetwork",
  "orderer": {
    "host": "orga.example.com",
    "port": 30500
  }
}
```

```json
{
  "networkName": "TestNetwork",
  "orderer": {
    "host": "orga.example.com",
    "port": 30501
  }
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "downloadUrl": "/download/certfile/ec3d1f09-5cf4-4e3e-804c-ae2a7edc3acc.zip"
  }
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "downloadUrl": "/download/certfile/2b018ac5-d2b8-4a64-87e8-182ecec89e28.zip"
  }
}
```

有了Orderer的证书和网络的创世区块，组织端就可以启动Orderer节点了。



### 3.2 查询网络信息

通过`/api/v1/network/queryNetworks`可以按网络名称关键词和组织名称关键词查询到所有相关的网络信息。

```json
{
  "networkNameKeyword": "TestNetwork",
  "organizationNameKeyword": "TestOrgA",
  "page": 1,
  "pageSize": 10
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "totalPages": 1,
    "items": [
      {
        "name": "TestNetwork",
        "orderers": [
          {
            "host": "orga.example.com",
            "port": 30500,
            "organizationName": "TestOrgA",
            "addr": "orga.example.com:30500"
          },
          {
            "host": "orga.example.com",
            "port": 30501,
            "organizationName": "TestOrgA",
            "addr": "orga.example.com:30501"
          }
        ],
        "organizationNames": [
          "TestOrgA"
        ],
        "consortiumName": "TestConsortium"
      }
    ]
  }
}
```



### 3.3 申请加入网络

不在网络中的组织可以通过`/api/v1/network/applyParticipation`向网络提交加入申请：

```json
{
  "description": "This is TestOrgB trying to take part in TestNetwork",
  "networkName": "TestNetwork"
}
```

注意要同时提交组织在该网络中的管理员证书。



### 3.4 查询加入网络申请

已经网络中的成员可以通过`/api/v1/network/queryParticipations`查询特定网络的加入申请（status为-1、0、1依次表示已拒绝、正在处理、已通过）：

```json
{
  "networkName": "TestNetwork",
  "page": 1,
  "pageSize": 10,
  "status": 0
}
```

同时，可以利用该接口来查询网络中存在哪些组织（设置status为1即可）。

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "totalPages": 1,
    "items": [
      {
        "networkName": "TestNetwork",
        "organizationName": "TestOrgB",
        "orderers": [
          {
            "host": "orgb.example.com",
            "port": 30503,
            "addr": "orgb.example.com:30503"
          }
        ],
        "status": 0,
        "description": "This is TestOrgB trying to take part in TestNetwork",
        "timestamp": 1650983646736,
        "approvals": []
      }
    ]
  }
}
```

`approvals`是组织名称的字符串数组，表示网络中的哪些组织已经通过了该加入网络的申请。



### 3.5 处理加入网络申请

已经在网络中的组织在查询知道其他组织想加入网络的申请之后，可以通过`/api/v1/network/handleParticipation`来处理：

```json
{
  "allowed": true,
  "networkName": "TestNetwork",
  "organizationName": "TestOrgB"
}
```

在TestOrgB加入成功后，如果TestOrgC想加入，就必须同时经过TestOrgA和TestOrgB的同意。



### 3.6 新增Orderer节点

成功加入网络的组织如果需要新增自己的Orderer节点，那么他需要通过`/api/v1/network/addOrderer`实现：

```json
{
  "networkName": "TestNetwork",
  "orderer": {
    "host": "orgb.example.com",
    "port": 30503
  }
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "downloadUrl": "/download/certfile/56a957de-2b8d-4622-a926-336135648f7d.zip"
  }
}
```

然后通过`/api/v1/network/queryGenesisBlock`查询新的创世区块：

```json
{
  "networkName": "TestNetwork"
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "downloadUrl": "/download/block/0e96a33d-c117-4c84-9839-c74de653227c.block"
  }
}
```

然后用这两样东西来启动新的Orderer即可。

