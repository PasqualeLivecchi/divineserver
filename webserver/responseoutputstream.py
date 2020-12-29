

# # plenty of room for improvement
# class ResponseOutputStream: # extends ByteArrayOutputStream {
#     def __init__(self, response):
#         if not response:
#             raise Exception("Response equals None")
#         self.response = response

#     def close(self): # throws IOError {
#         super.close()
#         size = size()
#         self.response.body["size"] = size
#         self.response.body["content"] = bytes(buf[0:size])
