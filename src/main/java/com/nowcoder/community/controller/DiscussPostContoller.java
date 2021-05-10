package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostContoller implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;


    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title, String content){
        User user = hostHolder.getUser();
        if (user == null){
            return CommunityUtil.getJsonString(403,"你还咩有登陆！");
        }
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        //报错情况会统一处理
        return CommunityUtil.getJsonString(0,"发布成功");
    }

    @RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page){
        //帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post",post);
        //作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user",user);

        //评论的分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(post.getCommentCount());

        // 评论：给帖子的评论
        // 回复：给评论的评论
        // 评论列表
        List<Comment> commentList = commentService .findCommentsByEntity(
                ENTITY_TYPE_POST,post.getId(),page.getOffset(),page.getLimit());
        // 评论的VO列表
        List<Map<String,Object>> commentVoList =  new ArrayList<>();
        if(commentList != null){

            for (Comment comment : commentList){
                // 评论VO
                Map<String,Object> commentVo = new HashMap<>();
                // 评论
                commentVo.put("comment",comment);
                // 作者
                commentVo.put("user",userService.findUserById(comment.getUserId()));

                // 回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(
                         ENTITY_TYPE_COMMENT,comment.getId(),0,Integer.MAX_VALUE);

                // 回复VO列表
                List<Map<String,Object>> replyVoList = new ArrayList<>();
                 if (replyList!=null){
                     for( Comment reply :replyList){
                            Map<String,Object> replyVo = new HashMap<>();
                            // 回复
                            replyVo.put("reply",reply);
                            // 作者
                            replyVo.put("user",userService.findUserById(reply.getUserId()));
                            // 回复目标
                            User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                            replyVo.put("target",target);

                            replyVoList.add(replyVo);
                     }
                     commentVo.put("replys",replyVoList);

                     // 回复数量
                     int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT,comment.getId());
                     commentVo.put("replyCount",replyCount);

                     commentVoList.add(commentVo);

                 }

            }
        }
        model.addAttribute("comments",commentVoList);
        return "/site/discuss-detail";
    }

}
