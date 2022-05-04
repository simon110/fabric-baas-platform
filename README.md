# 可信端接口说明



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
    "domain": "anhui.example.com",
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



### 3.7 查询网络详情

通过`/api/v1//api/v1/network/getNetwork`可以查询到指定名称网络的详细信息：

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
    "result": {
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
        },
        {
          "host": "orgb.example.com",
          "port": 30503,
          "organizationName": "TestOrgB",
          "addr": "orgb.example.com:30503"
        },
        {
          "host": "orgc.example.com",
          "port": 30506,
          "organizationName": "TestOrgC",
          "addr": "orgc.example.com:30506"
        },
        {
          "host": "orgd.example.com",
          "port": 30509,
          "organizationName": "TestOrgD",
          "addr": "orgd.example.com:30509"
        },
        {
          "host": "orgc.example.com",
          "port": 30507,
          "organizationName": "TestOrgC",
          "addr": "orgc.example.com:30507"
        },
        {
          "host": "orgb.example.com",
          "port": 30504,
          "organizationName": "TestOrgB",
          "addr": "orgb.example.com:30504"
        }
      ],
      "organizationNames": [
        "TestOrgA",
        "TestOrgB",
        "TestOrgC",
        "TestOrgD"
      ],
      "consortiumName": "TestConsortium"
    }
  }
}
```



### 3.8 查询网络所有通道

通过`/api/v1//api/v1/network/queryNetworkChannels`可以查询到指定网络中的所有通道信息：

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
    "items": [
      "testchannel",
      "samplechannel"
    ]
  }
}
```



### 3.9 查询Orderer TLS证书

通过`/api/v1/network/queryOrdererTlsCert`可以查询到网络中指定Orderer的TLS证书，即证书tls目录下的`ca.crt`文件：

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
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "downloadUrl": "/download/certfile/2a81ba6b-f1f1-4766-b438-864d169c269e.zip"
  }
}
```



### 3.10 查询网络所有组织

通过`/api/v1/network/queryOrganizations`可以查询到网络中的所有组织名称：

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
    "items": [
      "TestOrgA",
      "TestOrgB",
      "TestOrgC",
      "TestOrgD"
    ]
  }
}
```





### 3.11 查询网络所有Orderer

通过`/api/v1/network/queryOrderers`可以查询到网络中的所有Orderer信息：

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
    "items": [
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
      },
      {
        "host": "orgb.example.com",
        "port": 30503,
        "organizationName": "TestOrgB",
        "addr": "orgb.example.com:30503"
      },
      {
        "host": "orgc.example.com",
        "port": 30506,
        "organizationName": "TestOrgC",
        "addr": "orgc.example.com:30506"
      },
      {
        "host": "orgd.example.com",
        "port": 30509,
        "organizationName": "TestOrgD",
        "addr": "orgd.example.com:30509"
      },
      {
        "host": "orgc.example.com",
        "port": 30507,
        "organizationName": "TestOrgC",
        "addr": "orgc.example.com:30507"
      },
      {
        "host": "orgb.example.com",
        "port": 30504,
        "organizationName": "TestOrgB",
        "addr": "orgb.example.com:30504"
      }
    ]
  }
}
```



## 4 通道管理

### 4.1 创建通道

已经在网络中的组织可以通过`/api/v1/channel/createChannel`接口来创建通道：

```json
{
  "channelName": "samplechannel",
  "networkName": "TestNetwork"
}
```



### 4.2 Peer加入通道

创建通道完成后，组织端需要用自己的证书来先启动一个Peer。例如此处启动了一个地址为**orga.example.com:31000**的Peer，然后通过`/api/v1/channel/joinChannel`将Peer加入到通道**samplechannel**中，注意要求同时上传Peer的证书压缩包：

```json
{
  "channelName": "samplechannel",
  "peer": {
    "host": "orga.example.com",
    "port": 31000
  }
}
```



### 4.3 生成邀请码

通道中的组织可以通过`/api/v1/channel/generateInvitationCode`生成邀请码来邀请网络中的其他组织加入，例如组织TestOrgA要邀请TestOrgB加入**samplechannel**：

```json
{
  "channelName": "samplechannel",
  "organizationName": "TestOrgB"
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "result": "9ajLhC0nl7FQteTkLCZ7Sl4WoMVl/HN6SeY5KeNnYMQ7kqfwIwbp/b9oficgB7CYH7nLjY9jo8OSXkY0IGGW8A=="
  }
}
```

然后将该邀请码发送给TestOrgB相关人员即可。



### 4.4 提交邀请码

在收集到当前通道中所有组织的邀请码后，可以通过`/api/v1/channel/submitInvitationCodes`来提交所有的邀请码以加入通道，如果TestOrgC拿到TestOrgA和TestOrgB发给他的邀请码，则发送请求：

```json
{
  "channelName": "samplechannel",
  "invitationCodes": [
    "+B7dyt9lgp1lo/RaYWGSfkaTbxdd000MrUiY8bJc/wIjBBxu77S3C+5UIr/tf8Mf0HJQWAM4cnujOGVxkkn7Mg==",
    "sCJvEFrFCfjghMVCrPJDQUaTbxdd000MrUiY8bJc/wJbh1OsuaD1jgOE7YOROyy5fym8kjl/dhF9nhJdjOCCXg=="
  ]
}
```

提交了邀请码之后组织就是该通道的组织了，可以将他们的节点加入到通道中。



### 4.5 设置锚节点

在将Peer加入通道之后，可以通过`/api/v1/channel/setAnchorPeer`将其设置为锚节点：

```json
{
  "channelName": "samplechannel",
  "peer": {
    "host": "orga.example.com",
    "port": 31000
  }
}
```





### 4.6 询通道详细信息

通过`/api/v1//api/v1/channel/getChannel`可以查询到指定名称网络的详细信息：

```json
{
  "channelName": "samplechannel"
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "result": {
      "name": "samplechannel",
      "networkName": "TestNetwork",
      "peers": [
        {
          "host": "orga.example.com",
          "port": 31000,
          "name": "samplechannel-Peer0",
          "organizationName": "TestOrgA",
          "addr": "orga.example.com:31000"
        },
        {
          "host": "orgb.example.com",
          "port": 31003,
          "name": "samplechannel-Peer1",
          "organizationName": "TestOrgB",
          "addr": "orgb.example.com:31003"
        },
        {
          "host": "orgc.example.com",
          "port": 31006,
          "name": "samplechannel-Peer2",
          "organizationName": "TestOrgC",
          "addr": "orgc.example.com:31006"
        },
        {
          "host": "orgc.example.com",
          "port": 31007,
          "name": "samplechannel-Peer3",
          "organizationName": "TestOrgC",
          "addr": "orgc.example.com:31007"
        }
      ],
      "organizationNames": [
        "TestOrgA",
        "TestOrgB",
        "TestOrgC"
      ]
    }
  }
}
```



### 4.7 查询Peer TLS证书

通过`/api/v1/network/queryPeerTlsCert`可以查询到通道中指定Peer的TLS证书，即证书tls目录下的`ca.crt`文件：

```json
{
  "channelName": "samplechannel",
  "peer": {
    "host": "orga.example.com",
    "port": 31000
  }
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "downloadUrl": "/download/cert/6ddaf058-fc8d-47a4-94a9-38c7916f43b5.crt"
  }
}
```



### 4.8 查询通道所有Peer

通过`/api/v1/network/queryPeers`可以查询到通道中所有Peer的信息：

```json
{
  "channelName": "samplechannel"
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "items": [
      {
        "host": "orga.example.com",
        "port": 31000,
        "name": "samplechannel-Peer0",
        "organizationName": "TestOrgA",
        "addr": "orga.example.com:31000"
      },
      {
        "host": "orgb.example.com",
        "port": 31003,
        "name": "samplechannel-Peer1",
        "organizationName": "TestOrgB",
        "addr": "orgb.example.com:31003"
      },
      {
        "host": "orgc.example.com",
        "port": 31006,
        "name": "samplechannel-Peer2",
        "organizationName": "TestOrgC",
        "addr": "orgc.example.com:31006"
      },
      {
        "host": "orgc.example.com",
        "port": 31007,
        "name": "samplechannel-Peer3",
        "organizationName": "TestOrgC",
        "addr": "orgc.example.com:31007"
      }
    ]
  }
}
```





# 组织端接口说明

## 0 注意事项

测试环境的集群包括172.18.118.183到172.18.118.186四个节点，集群DNS和所有测试机器的hosts中配置了四个相应可访问的虚拟域名，分别为`orga.example.com`、`orgb.example.com`、`orgc.example.com`、`orgd.example.com`。其他域名均不可访问，在初始化组织端域名的时候请选择这四个域名中的一个。

集群的合法端口范围为[30000, 32767]，其中30000端口已经被Kubernetes Dashboard占用了。

由于测试环境四个组织公用的是一个集群，所以不同组织的Peer和Orderer名称必须起不一样的，端口也不能重复。







## 1 系统管理

### 1.1 管理员登录

首先第一步需要通过`/api/v1/user/login`先登录系统：

```json
{
  "organizationName": "admin",
  "password": "5NjLjNZQUSTKKoS0"
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJleHAiOjE2NTE1NzY0NDYsImF1dGhvcml0aWVzIjoiUk9MRV9BRE1JTiIsInVzZXJuYW1lIjoiYWRtaW4ifQ.lCZkEMbbw7G5w-HFuWja1uG-Eg9pbbAMLsoOy0Aewhp7hN9qWLOuJF-ACF-exQIgGK08ZZvFEiOgWkjjPvDIqA"
  }
}
```

同样将token设置为HTTP请求头的Authorization字段来获得访问权限。系统不支持注册新账户，只能通过管理员进行操作。



### 1.2 系统状态

登录后需要通过`/api/v1/system/isAvailable`来判断系统是否可用，不需要传入任何参数：

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "result": false
  }
}
```

如果返回的结果为false需要先对系统进行初始化。



### 1.3 系统初始化

通过`/api/v1/system/init`来对系统进行初始化，需要传入的参数包括**Kubernetes的管理员证书文件**、**组织CA的基本信息**、**TTP端的登录信息**和**新的组织端密码（可选）**，请求内容如下：

```json
{
  "adminPassword": "12345678",
  "org": {
    "countryCode": "CN",
    "domain": "orga.example.com",
    "locality": "Guangzhou",
    "organizationName": "TestOrgA",
    "stateOrProvince": "Guangdong"
  },
  "remoteUser": {
    "apiServer": "172.18.118.222:8080",
    "organizationName": "TestOrgA",
    "password": "12345678"
  }
}
```

其中，domain必须为其他组织连接该组织集群的正确域名，不能随便设置；remoteUser表示与TTP端通信的身份，该账号必须已经通过TTP端的审核，确保可以正常登录。



### 1.4 查询集群节点

通过`/api/v1/system/getClusterNodeNames`可以查询到上传证书对应的Kubernetes集群中有哪些物理节点：

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "items": [
      "kubemaster",
      "kubenode1",
      "kubenode2",
      "kubenode3"
    ]
  }
}
```

请勿选择Master节点来部署容器，Master节点不应该且拒绝被部署任何容器。





## 2 证书管理

### 2.1 证书注册

在使用证书前，需要先通过`/api/v1/cert/generate`接口来向本地CA注册证书，主要用于注册启动Peer节点的证书，例如：

```json
{
  "caUsername": "TestOrgAPeer0",
  "caPassword": "12345678",
  "caUsertype": "peer"
}
```

注意caUsertype必须为admin、client、peer和orderer中的一种。



### 2.2 证书查询

通过`/api/v1/cert/query`可以按证书类型分页查询到已经在本地CA注册过的所有证书：

```json
{
  "page": 1,
  "pageSize": 10,
  "usertype": "peer"
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
        "caUsername": "TestOrgAPeer0",
        "caPassword": "Not Available",
        "caUsertype": "peer"
      },
      {
        "caUsername": "TestOrgAPeer1",
        "caPassword": "Not Available",
        "caUsertype": "peer"
      },
      {
        "caUsername": "TestOrgAPeer2",
        "caPassword": "Not Available",
        "caUsertype": "peer"
      }
    ]
  }
}
```



## 3 网络管理

### 3.1 网络创建

通过`/api/v1/network/create`可以向TTP端请求创建网络并启动Orderer：

```json
{
  "consortiumName": "SampleConsortium",
  "networkName": "SampleNetwork",
  "orderers": [
    {
      "kubeNodeName": "kubenode1",
      "kubeNodePort": 30500,
      "name": "TestOrgAOrderer0"
    },
    {
      "kubeNodeName": "kubenode2",
      "kubeNodePort": 30501,
      "name": "TestOrgAOrderer1"
    }
  ]
}
```

该接口会自动从TTP端下载创世区块和Orderer证书，并将Orderer部署到指定的节点上。kubeNodeName为集群中任意非Master节点的名称，kubeNodePort为Orderer节点与外部通信的接口。外界访问本组织的地址为初始化系统时填入的domain加上kubeNodePort，例如此处访问TestOrgAOrderer0的地址为`orga.example.com:30500`。

如果确认集群中是否已经成功部署Orderer可以访问 https://172.18.118.185:30000/#/login 后进行确认，登录Token为：

```
eyJhbGciOiJSUzI1NiIsImtpZCI6IndnQVpfTGR0d1pOSHd0V3Nsc1FJRUE5UmhGektzYjRUcHRnYVJ1cmtFUDgifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJkYXNoYm9hcmQtYWRtaW4tdG9rZW4tMnp3bHEiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoiZGFzaGJvYXJkLWFkbWluIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQudWlkIjoiMmU5NDUzNTItNWRmMC00YWExLTk2MTUtYTIxZTI4MTNiN2NkIiwic3ViIjoic3lzdGVtOnNlcnZpY2VhY2NvdW50Omt1YmUtc3lzdGVtOmRhc2hib2FyZC1hZG1pbiJ9.WM45cxmV4B9hYc2VKle8tnfu7ibceWLKub-El9EqkhtuMMQkD9pi-ohJcmZnavljxsLBdPP6_heBVmV8mpFMgfl6GWKyCsxUr-VdPyjAQ3Ktq3-aa2Ix6maV4TWerER-UUXSZpE9eK4akJRicdni7WsmJqjOgPUK-ywzJMjyYYrAj60CC4cP_lpm0iTsfnLC0YjewWiIaV1Y8zsMzTMsejJY0zNflzkghNUjloDyXTfL6efFHTPM0WtkGiLw18h6Rd-DO8QqSptZ4hejasLuAKLpLAneiBfc244NOZ9UtR6JAmBQLFpLeJjc9APIrihpXFYfeeflEic-cHZhloUn9A
```



### 3.2 申请加入网络

该接口直接将请求转发到TTP端，此处我们假设TestOrgB向TTP申请加入上面创建的`HaloNetwork`，调用`/api/v1/network/applyParticipation`接口即可，该接口会自动将当前组织端的证书打包上传：

```json
{
  "description": "This is TestOrgB trying to take part in HaloNetwork",
  "networkName": "HaloNetwork"
}
```



### 3.3 查询加入网络申请

在组织端可以通过`/api/v1/network/queryParticipations`来查询特定网络加入组织的申请：

```json
{
  "networkName": "HaloNetwork",
  "page": 1,
  "pageSize": 10,
  "status": 0
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
        "networkName": "HaloNetwork",
        "status": 0,
        "description": "This is TestOrgB trying to take part in HaloNetwork"
      }
    ]
  }
}
```



### 3.4 同意加入网络申请

在其他组织提交了加入网络的申请后，TestOrgA可以通过`/api/v1/network/handleParticipation`来处理其他组织的申请，例如：

```json
{
  "accepted": true,
  "networkName": "HaloNetwork",
  "organizationName": "TestOrgB"
}
```

需要所有在`HaloNetwork`中的组织同意，一旦所有组织都同意之后，申请的组织会被写入到区块链的联盟配置中。



### 3.5 添加Orderer

如果后续想要向网络中添加Orderer，可以先通过`/api/v1/network/addOrderer`来向TTP注册一个新的Orderer，例如TestOrgA注册一个地址为`orga.example.com:30502`的新Orderer：

```json
{
  "networkName": "HaloNetwork",
  "ordererPort": 30502
}
```

注册成功之后，通过`/api/v1/orderer/startOrderer`来启动相应的Orderer即可：

```json
{
  "kubeNodeName": "kubenode3",
  "kubeNodePort": 30502,
  "name": "TestOrgAOrderer2",
  "networkName": "HaloNetwork"
}
```

然后一个新的Orderer节点便会被部署到集群中。



### 3.6 查询网络Orderer节点

通过`/api/v1/orderer/queryOrderersInNetwork`可以查询到指定网络中所有的Orderer节点：

```json
{
  "networkName": "HaloNetwork"
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "items": [
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
      },
      {
        "host": "orga.example.com",
        "port": 30502,
        "organizationName": "TestOrgA",
        "addr": "orga.example.com:30502"
      }
    ]
  }
}
```



### 3.7 查询本地Orderer节点

通过`/api/v1/orderer/queryOrderersInCluster`可以查询到指定当前组织的集群中所有的Orderer节点：

```json
{
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
        "name": "TestOrgAOrderer0",
        "kubeNodeName": "kubenode1",
        "kubeNodePort": 30500
      },
      {
        "name": "TestOrgAOrderer1",
        "kubeNodeName": "kubenode2",
        "kubeNodePort": 30501
      },
      {
        "name": "TestOrgAOrderer2",
        "kubeNodeName": "kubenode3",
        "kubeNodePort": 30502
      }
    ]
  }
}
```



### 3.8 查询所有网络

通过`/api/v1/network/getParticipatedNetworks`可以查询当前组织已经加入的所有网络。

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "items": [
      {
        "orderers": [
          {
            "organizationName": "TestOrgA",
            "port": 30500,
            "host": "orga.example.com",
            "addr": "orga.example.com:30500"
          },
          {
            "organizationName": "TestOrgA",
            "port": 30501,
            "host": "orga.example.com",
            "addr": "orga.example.com:30501"
          },
          {
            "organizationName": "TestOrgA",
            "port": 30502,
            "host": "orga.example.com",
            "addr": "orga.example.com:30502"
          }
        ],
        "organizationNames": [
          "TestOrgA",
          "TestOrgB"
        ],
        "consortiumName": "HaloConsortium",
        "name": "HaloNetwork"
      },
      {
        "orderers": [
          {
            "organizationName": "TestOrgA",
            "port": 30500,
            "host": "orga.example.com",
            "addr": "orga.example.com:30500"
          },
          {
            "organizationName": "TestOrgA",
            "port": 30501,
            "host": "orga.example.com",
            "addr": "orga.example.com:30501"
          }
        ],
        "organizationNames": [
          "TestOrgA"
        ],
        "consortiumName": "SampleConsortium",
        "name": "SampleNetwork"
      }
    ]
  }
}
```





## 4 通道管理

### 4.1 创建通道

在没有启动Peer的情况下，可以先通过`/api/v1/channel/create`创建一个新的通道，一个通道只属于一个网络，每个网络可以包含多个通道，通道创建时只包含创建组织。注意通道名只能包含小写字母和数字，且以小写字母开头，例如：

```json
{
  "channelName": "halochannel",
  "networkName": "HaloNetwork"
}
```



### 4.2 启动Peer

在创建通道成功后，可以通过`/api/v1/orderer/startOrderer`启动Peer。启动前请先确认已经注册了相应的Peer证书，因为此处需要提供对应证书的账户和密码：

```json
{
  "caPassword": "12345678",
  "caUsername": "TestOrgAPeer0",
  "couchDBPassword": "12345678",
  "couchDBUsername": "admin",
  "kubeEventNodePort": 31500,
  "kubeNodeName": "kubenode1",
  "kubeNodePort": 31000,
  "name": "TestOrgAPeer0"
}
```

CouchDB的用户密码为自定义，每个Peer维护一个独立的CouchDB。注意Peer有两个端口是外部可以访问的，kubeNodePort是主端口，用来和其他节点通信的；kubeEventNodePort是事件端口，是让SDK去监听区块链事件的。这两个端口必须不同。name字段是组织端内部用来唯一标识Peer的。



### 4.3 加入通道

启动Peer之后，可以通过`/api/v1/channel/join`将Peer加入到已经创建的通道中，例如此处将TestOrgAPeer0加入到已经创建的通道`halochannel`中：

```json
{
  "channelName": "halochannel",
  "peerName": "TestOrgAPeer0"
}
```



### 4.4 查询通道Peer节点

通过`/api/v1/peer/queryPeersInChannel`可以查询到指定通道中所有的Peer节点：

```json
{
  "channelName": "halochannel"
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "items": [
      {
        "host": "orga.example.com",
        "port": 31000,
        "organizationName": "TestOrgA",
        "addr": "orga.example.com:31000"
      }
    ]
  }
}
```



### 4.5 查询本地Peer节点

通过`/api/v1/peer/queryPeersInCluster`可以查询到指定当前组织的集群中所有的Peer节点：

```json
{
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
        "name": "TestOrgAPeer0",
        "kubeNodeName": "kubenode1",
        "kubeNodePort": 31000,
        "kubeEventNodePort": 31500,
        "caUsername": "TestOrgAPeer0",
        "caPassword": "Not Available",
        "couchDBUsername": "admin",
        "couchDBPassword": "Not Available"
      }
    ]
  }
}
```



### 4.6 生成邀请码

通过`/api/v1/channel/generateInvitationCode`可以生成邀请特定组织进入通道的邀请码：

```json
{
  "channelName": "halochannel",
  "invitedOrganizationName": "TestOrgB"
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "result": "iuvvlFfBnaJhW2ZQAAe7kNhrDAfPreeIuPqudl5Y1JrE5/tRdp1g4TS3M+/4xAayJGlR/GlHCkRyJ3adBxwqYQ=="
  }
}
```



### 4.7 提交邀请码

通过`/api/v1/channel/submitInvitationCodes`可以将从其他全部组织处获得的邀请码提交以加入特定的通道，例如被邀请的组织TestOrgB应提交：

```json
{
  "channelName": "halochannel",
  "invitationCodes": [
    "iuvvlFfBnaJhW2ZQAAe7kNhrDAfPreeIuPqudl5Y1JrE5/tRdp1g4TS3M+/4xAayJGlR/GlHCkRyJ3adBxwqYQ=="
  ]
}
```



### 4.8 设置锚节点

已经加入通道的Peer可以通过`/api/v1/channel/updateAnchor`被设置为组织的锚节点（负责与其他组织通信）。注意，一个锚节点一旦被设置将永远是锚节点，但一个组织允许设置多个锚节点。

```json
{
  "channelName": "halochannel",
  "peerName": "TestOrgAPeer0"
}
```



### 4.9 查询所有通道

通过`/api/v1/channel/getParticipatedChannels`可以查询到当前组织参与的所有通道。

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "items": [
      {
        "networkName": "HaloNetwork",
        "peers": [
          {
            "organizationName": "TestOrgA",
            "port": 31000,
            "host": "orga.example.com",
            "name": "halochannel-orgaexamplecom-31000",
            "addr": "orga.example.com:31000"
          }
        ],
        "organizationNames": [
          "TestOrgA",
          "TestOrgB"
        ],
        "name": "halochannel"
      },
      {
        "networkName": "HaloNetwork",
        "peers": [
          {
            "organizationName": "TestOrgA",
            "port": 31000,
            "host": "orga.example.com",
            "name": "testchannel-orgaexamplecom-31000",
            "addr": "orga.example.com:31000"
          }
        ],
        "organizationNames": [
          "TestOrgA"
        ],
        "name": "testchannel"
      }
    ]
  }
}
```



## 5 链码管理

### 5.1 链码安装

通过`/api/v1/chaincode/install`可以将链码安装到Peer上，必须上传一份已经编译好的链码文件。可以使用项目example/fabric下面提供的编译好的链码压缩包直接上传。

```json
{
  "chaincodeLabel": "asset-transfer-ledger-queries-1.0",
  "peerName": "TestOrgAPeer0"
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "result": "asset-transfer-ledger-queries-1.0:e532fbdb5fa1cc81dee3388a665d9bee224bfa63c9f943247143f82c959382b8\n"
  }
}
```

返回值为链码的唯一标识符，后续会用到。所有通道中的组织都必须安装相同的链码才能继续后面的流程。



### 5.2 组织已安装链码查询

通过`/api/v1/chaincode/queryInstalledChaincodes`可以查询到当前组织端安装过的所有链码：

```json
{
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
        "identifier": "1.0:974fe7b8e06675a7f470c97e1a2a4b6450fe429eb0529183368263e8c978b126",
        "label": "asset-transfer-basic-1.0",
        "peerName": "TestOrgAPeer0"
      },
      {
        "identifier": "asset-transfer-ledger-queries-1.0:e532fbdb5fa1cc81dee3388a665d9bee224bfa63c9f943247143f82c959382b8",
        "label": "asset-transfer-ledger-queries-1.0",
        "peerName": "TestOrgAPeer0"
      }
    ]
  }
}
```



### 5.3 Peer已安装链码查询

通过`/api/v1/chaincode/getAllInstalledChaincodesOnPeer`可以查询到已经安装到指定Peer上的所有链码

```json
{
  "peerName": "TestOrgAPeer0"
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "items": [
      {
        "identifier": "1.0:974fe7b8e06675a7f470c97e1a2a4b6450fe429eb0529183368263e8c978b126",
        "label": "asset-transfer-basic-1.0",
        "peerName": "TestOrgAPeer0"
      },
      {
        "identifier": "asset-transfer-ledger-queries-1.0:e532fbdb5fa1cc81dee3388a665d9bee224bfa63c9f943247143f82c959382b8",
        "label": "asset-transfer-ledger-queries-1.0",
        "peerName": "TestOrgAPeer0"
      }
    ]
  }
}
```



### 5.4 链码投票

通过`/api/v1/chaincode/approve`可以对已经安装的链码进行投票：

```json
{
  "channelName": "testchannel",
  "installedChaincodeIdentifier": "asset-transfer-ledger-queries-1.0:e532fbdb5fa1cc81dee3388a665d9bee224bfa63c9f943247143f82c959382b8",
  "name": "asset-transfer-ledger-queries-chaincode",
  "peerName": "TestOrgAPeer0",
  "sequence": 1,
  "version": "1.0"
}
```

installedChaincodeIdentifier为安装链码时返回的链码编号，name表示支持该链码以什么名称被部署到通道上，channelName表示支持该链码在什么通道上生效，peerName对应的必须是当前组织中一个已经安装了对应链码的Peer节点，version表示链码的版本号，sequence表示这是链码的第几个版本（从1开始递增，每次升级链码都+1）。

所有的组织都必须对相应的链码进行投票且参数必须一致才能让链码生效。



### 5.5 组织投票记录

通过`/api/v1/chaincode/queryApprovedChaincodes`可以查询当前组织投票过的链码

```json
{
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
        "name": "asset-transfer-abac-chaincode",
        "version": "1.0",
        "sequence": 1,
        "channelName": "testchannel",
        "peerName": "TestOrgAPeer0",
        "installedChaincodeIdentifier": "asset-transfer-abac-1.0:db4ca8f29606a98dc7dc6e83de096545a7b5e9194f09769316125c13aae51eb6"
      },
      {
        "name": "asset-transfer-ledger-queries-chaincode",
        "version": "1.0",
        "sequence": 1,
        "channelName": "testchannel",
        "peerName": "TestOrgAPeer0",
        "installedChaincodeIdentifier": "asset-transfer-ledger-queries-1.0:e532fbdb5fa1cc81dee3388a665d9bee224bfa63c9f943247143f82c959382b8"
      }
    ]
  }
}
```



### 5.7 链码投票情况查询

可以通过`/api/v1/chaincode/getChaincodeApprovals`来对链码的投票情况进行查询：

```json
{
  "channelName": "testchannel",
  "name": "asset-transfer-ledger-queries-chaincode",
  "sequence": 1,
  "version": "1.0"
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "items": [
      {
        "organizationName": "TestOrgA",
        "approved": true
      },
      {
        "organizationName": "TestOrgB",
        "approved": false
      },
    ]
  }
}
```

必须在所有通道中组织的approved都为true链码才能被commit。



### 5.8 链码提交（生效）

所有组织都对链码进行投票之后，需要由其中的任意组织通过`/api/v1/chaincode/commit`来让链码生效，必须预先知道所有其他组织安装了链码的Peer地址，因为需要他们的背书。

```json
{
  "channelName": "testchannel",
  "endorserPeers": [
    {
      "host": "orgb.example.com",
      "port": 31005
    }
  ],
  "name": "asset-transfer-ledger-queries-chaincode",
  "sequence": 1,
  "version": "1.0"
}
```

commit完成后所有的组织的链码都会同时生效。



### 5.9 组织已提交的链码查询

通过`/api/v1/chaincode/queryCommittedChaincodes`可以查询到当前组织端已经生效的链码：

```json
{
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
        "name": "asset-transfer-ledger-queries-chaincode",
        "version": "1.0",
        "sequence": 1,
        "channelName": "testchannel",
        "peerName": "TestOrgAPeer0"
      }
    ]
  }
}
```



### 5.10 通道已提交的链码查询

通过`/api/v1/chaincode/getAllCommittedChaincodesOnChannel`可以查询到当前组织在指定Channel上的所有已经生效的链码

```json
{
  "channelName": "testchannel"
}
```

```json
{
  "code": 200,
  "message": "成功调用服务",
  "data": {
    "items": [
      {
        "name": "asset-transfer-ledger-queries-chaincode",
        "version": "1.0",
        "sequence": 1,
        "channelName": "testchannel",
        "peerName": "TestOrgAPeer0"
      }
    ]
  }
}
```

