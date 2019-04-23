# JenkinsStream

Purpose: provide a flexible, automated, declarative approach to bringing up a neptune environment.

## Overview

The api currently supports 4 operations:

* CreateTenant
* DeployStack
* DeployComponent
* DestroyTenant

## Details

You can combine these operations in a pipeline where one job conditionally executes after the next.
### Steps
1. Job is triggered 
2. Returns queueLocation 
3. Extract queueId from queueLocation 
4. Poll until a recent job has a matching queueId 
5. If a build matches, extract build information and return build info

What makes this library useful is the simple syntax. To create a tenant and deploy two stacks: 

```
for {
    createTenant <- jenkinsClient.createTenant("engage", "3 hours", "dev")
    tenant = createTenant.description
    engageStack <- jenkinsClient.deployStack(tenant, "engage")
    engineStack <- jenkinsClient.deployStack(tenant, "engine")
  } yield ???

```

#### Error Handling
What happens if a job finishes, but fails? Well, that's entirely up to you.

In the library, there are some error handlers already defined for you. They are:
* stopOnNonSuccessfulBuild
* continueOnNonSuccessfulBuild

You can pass a handler to the job being run. That determines how you want to handle a non-successful build. 

```
implicit val continueOnNonSuccessfulBuild
for {
    createTenant <- jenkinsClient.createTenant("engage", "3 hours", "dev")(stopOnNonSuccessfulBuild)
    tenant = createTenant.description
    engageStack <- jenkinsClient.deployStack(tenant, "engage")
    engineStack <- jenkinsClient.deployStack(tenant, "engine")
  } yield ???

```

The above code will create a tenant, and if the tenant is create successfully, the rest of the stacks will be deployed. But if the engage stack fails to finish, the engine stack job will still be triggered.

You can have any number of conditions as long as the condition can be determined from the BuildInfo
