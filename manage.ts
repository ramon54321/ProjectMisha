import shell from "shelljs"
import { S3 } from "@aws-sdk/client-s3"
import { createWriteStream, existsSync, mkdirSync } from "fs"
import { Stream } from "stream"
import { dirname } from "path"

class DataSource {
  private Bucket = "projectmisha"
  private client = new S3({
    region: "eu-central-1",
  })
  
  async getObjectKeys(): Promise<string[]> {
    const response = await this.client.listObjects({
      Bucket: this.Bucket,
    })
    return response.Contents?.map(entry => entry.Key!) || []
  }
  
  async getObjectStreamByKey(key: string): Promise<Stream> {
    const response = await this.client.getObject({
      Bucket: this.Bucket,
      Key: key,
    })
    return response.Body as Stream
  }
}

const RESOURCES_PATH = "client/resources"

const dataSource = new DataSource()

const actions = {
  pull: pull
} as any

const action = actions[process.argv[2]]
if (action) action()

async function pull() {
  if(!existsSync(RESOURCES_PATH)) mkdirSync(RESOURCES_PATH)
  const keys = await dataSource.getObjectKeys()
  keys.filter(key => !key.endsWith("/")).forEach(async key => {
    const path = `${RESOURCES_PATH}/${key}`
    console.log(`Writing to ${path}`)
    shell.mkdir('-p', dirname(path))
    const objectStream = await dataSource.getObjectStreamByKey(key)
    objectStream.pipe(createWriteStream(path))
  })
}
